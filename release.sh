#!/usr/bin/env bash
# ==============================================================================
# Codex Release Automation Script
# ==============================================================================
# Automates the release process for Codex Android app:
# - Interactive version selection (major/minor/patch bump)
# - Updates build.gradle.kts and README.md
# - Builds signed APK + generates SHA256 checksum
# - Uploads APK to GitLab Packages (if GITLAB_TOKEN set)
# - Creates git commit and annotated tag with changelog
# - Pushes to all remotes (github, gitlab, codeberg)
#
# Usage:
#   ./release.sh                    # Interactive mode (default)
#   ./release.sh --query            # Show current version and exit
#   ./release.sh --version 2.0.1    # Release specific version
#   ./release.sh --dry-run          # Preview without making changes
#   ./release.sh --debug            # Show detailed build output
#   ./release.sh --major            # Auto-select major version bump
#   ./release.sh --minor            # Auto-select minor version bump
#   ./release.sh --patch            # Auto-select patch version bump
#   ./release.sh --force            # Skip git status check
#
# Setup:
#   1. Copy .env.example to .env
#   2. Fill in your GitLab token and project ID
#   3. Set file permissions: chmod 600 .env
#
# Environment Variables:
#   GITLAB_TOKEN=your_token         # Enable automatic APK upload to GitLab Packages
#   GITLAB_PROJECT_ID=12345678      # Your GitLab project ID
# ==============================================================================

set -euo pipefail  # Exit on error, undefined vars, pipe failures

# ==============================================================================
# CONFIGURATION
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${SCRIPT_DIR}"
BUILD_GRADLE="${PROJECT_ROOT}/app/build.gradle.kts"
README="${PROJECT_ROOT}/README.md"
BACKUP_DIR="${PROJECT_ROOT}/.release_backup"
LOGS_DIR="${PROJECT_ROOT}/logs"
REMOTES=("github" "gitlab" "codeberg")

# ANSI color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'  # No Color

# Global state variables
DRY_RUN=false
FORCE=false
QUERY_ONLY=false
DEBUG=false
AUTO_BUMP=""
SPECIFIED_VERSION=""
CURRENT_VERSION=""
CURRENT_VERSION_CODE=""
NEW_VERSION=""
NEW_VERSION_CODE=""
PREVIOUS_TAG=""
CHANGELOG=""
GITLAB_APK_URL=""
ROLLBACK_NEEDED=false
TAG_CREATED=false
COMMIT_CREATED=false

# ==============================================================================
# UTILITY FUNCTIONS
# ==============================================================================

log_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

log_success() {
    echo -e "${GREEN}✓${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

log_error() {
    echo -e "${RED}✗${NC} $1"
}

log_header() {
    echo ""
    echo -e "${BOLD}$1${NC}"
    echo "========================================"
}

prompt_yes_no() {
    local prompt="$1"
    local default="${2:-N}"

    if [[ "$default" == "Y" ]]; then
        read -p "$prompt [Y/n]: " response
        [[ -z "$response" || "$response" =~ ^[Yy]$ ]]
    else
        read -p "$prompt [y/N]: " response
        [[ "$response" =~ ^[Yy]$ ]]
    fi
}

query_version() {
    # Parse current version without all the checks
    if [[ ! -f "${BUILD_GRADLE}" ]]; then
        log_error "Not in Codex project root. Expected to find: app/build.gradle.kts"
        exit 1
    fi

    local version_code
    local version_name
    local latest_tag

    version_code=$(grep "^\s*versionCode = " "${BUILD_GRADLE}" | sed 's/.*= *//' | tr -d ' ')
    version_name=$(grep "^\s*versionName = " "${BUILD_GRADLE}" | sed 's/.*"\(.*\)".*/\1/')
    latest_tag=$(git describe --tags --abbrev=0 2>/dev/null || echo "none")

    log_header "Current Codex Version"
    echo "Version Name: ${version_name}"
    echo "Version Code: ${version_code}"
    echo "Latest Tag:   ${latest_tag}"
    echo ""

    # Show what the next versions would be
    local major minor patch
    IFS='.' read -r major minor patch <<< "$version_name"
    major="${major%%-*}"
    minor="${minor%%-*}"
    patch="${patch%%-*}"

    echo "Next versions would be:"
    echo "  Major: $((major + 1)).0.0 (code: $((version_code + 1)))"
    echo "  Minor: ${major}.$((minor + 1)).0 (code: $((version_code + 1)))"
    echo "  Patch: ${major}.${minor}.$((patch + 1)) (code: $((version_code + 1)))"
    echo ""

    exit 0
}

# ==============================================================================
# ERROR HANDLING AND ROLLBACK
# ==============================================================================

rollback() {
    local error_msg="$1"

    if [[ "$ROLLBACK_NEEDED" == "false" ]]; then
        return
    fi

    log_error "ERROR: $error_msg"
    log_warning "Rolling back changes..."

    # Restore backups if they exist
    if [[ -f "${BACKUP_DIR}/build.gradle.kts.bak" ]]; then
        cp "${BACKUP_DIR}/build.gradle.kts.bak" "${BUILD_GRADLE}"
        log_info "- Restored build.gradle.kts"
    fi

    if [[ -f "${BACKUP_DIR}/README.md.bak" ]]; then
        cp "${BACKUP_DIR}/README.md.bak" "${README}"
        log_info "- Restored README.md"
    fi

    # Unstage and checkout modified files
    git reset HEAD "${BUILD_GRADLE}" "${README}" 2>/dev/null || true
    git checkout -- "${BUILD_GRADLE}" "${README}" 2>/dev/null || true

    # Delete tag if created locally
    if [[ "$TAG_CREATED" == "true" && -n "$NEW_VERSION" ]]; then
        if git tag -l | grep -q "^v${NEW_VERSION}$"; then
            git tag -d "v${NEW_VERSION}" 2>/dev/null || true
            log_info "- Deleted tag v${NEW_VERSION}"
        fi
    fi

    # Reset commit if created but not pushed
    if [[ "$COMMIT_CREATED" == "true" && -n "$NEW_VERSION" ]]; then
        local last_commit_msg
        last_commit_msg=$(git log -1 --format=%s 2>/dev/null || echo "")
        if [[ "$last_commit_msg" == "Release v${NEW_VERSION}" ]]; then
            git reset --soft HEAD~1 2>/dev/null || true
            log_info "- Reverted commit"
        fi
    fi

    log_warning "Rollback complete. Safe to re-run script."
    exit 1
}

cleanup_on_exit() {
    # Don't delete backups - keep them for debugging
    :
}

trap 'rollback "Script failed at line $LINENO"' ERR
trap cleanup_on_exit EXIT

# ==============================================================================
# PRE-FLIGHT CHECKS
# ==============================================================================

check_working_directory() {
    log_info "Checking working directory..."

    if [[ ! -f "${BUILD_GRADLE}" ]]; then
        log_error "Not in Codex project root. Expected to find: app/build.gradle.kts"
        exit 1
    fi

    log_success "Working directory verified"
}

check_git_status() {
    if [[ "$FORCE" == "true" ]]; then
        log_warning "Skipping git status check (--force flag)"
        return
    fi

    log_info "Checking git status..."

    local status
    status=$(git status --porcelain)

    if [[ -n "$status" ]]; then
        log_warning "Working directory has uncommitted changes:"
        echo "$status"
        echo ""

        if ! prompt_yes_no "Continue anyway? (Changes will be stashed)"; then
            log_info "Aborted by user"
            exit 0
        fi

        log_info "Stashing changes..."
        git stash push -m "release.sh auto-stash $(date +%Y-%m-%d_%H:%M:%S)"
        log_success "Changes stashed"
    else
        log_success "Working directory is clean"
    fi
}

check_keystore() {
    log_info "Checking keystore configuration..."

    if [[ ! -f "${PROJECT_ROOT}/keystore.properties" ]]; then
        log_error "keystore.properties not found"
        log_error "Cannot sign release APK without keystore configuration"
        exit 1
    fi

    local keystore_file
    keystore_file=$(grep "storeFile=" "${PROJECT_ROOT}/keystore.properties" | cut -d'=' -f2)

    if [[ ! -f "${PROJECT_ROOT}/${keystore_file}" ]]; then
        log_error "Keystore file not found: ${keystore_file}"
        log_error "Cannot sign release APK"
        exit 1
    fi

    log_success "Keystore configuration verified"
}

check_gradle() {
    log_info "Checking Gradle wrapper..."

    if [[ ! -x "${PROJECT_ROOT}/gradlew" ]]; then
        log_error "Gradle wrapper not found or not executable"
        exit 1
    fi

    log_success "Gradle wrapper verified"
}

check_remotes() {
    log_info "Checking git remotes..."

    local all_ok=true

    for remote in "${REMOTES[@]}"; do
        if ! git remote get-url "${remote}" &>/dev/null; then
            log_warning "Remote '${remote}' not configured"
            all_ok=false
        fi
    done

    if [[ "$all_ok" == "true" ]]; then
        log_success "All remotes configured"
    else
        log_warning "Some remotes are missing (will skip during push)"
    fi
}

check_tag_exists() {
    local tag="v${NEW_VERSION}"

    # Check local tags
    if git tag -l | grep -q "^${tag}$"; then
        log_error "Tag ${tag} already exists locally"

        if prompt_yes_no "Delete existing tag and continue?"; then
            git tag -d "${tag}"
            log_success "Deleted local tag ${tag}"
        else
            log_info "Aborted by user"
            exit 0
        fi
    fi

    # Check remote tags
    for remote in "${REMOTES[@]}"; do
        if git remote get-url "${remote}" &>/dev/null; then
            if git ls-remote --tags "${remote}" | grep -q "refs/tags/${tag}$"; then
                log_error "Tag ${tag} already exists on remote '${remote}'"
                log_error "Delete the remote tag manually before proceeding"
                exit 1
            fi
        fi
    done
}

# ==============================================================================
# VERSION MANAGEMENT
# ==============================================================================

parse_current_version() {
    log_info "Parsing current version..."

    # Extract versionCode
    CURRENT_VERSION_CODE=$(grep "^\s*versionCode = " "${BUILD_GRADLE}" | sed 's/.*= *//' | tr -d ' ')

    # Extract versionName
    CURRENT_VERSION=$(grep "^\s*versionName = " "${BUILD_GRADLE}" | sed 's/.*"\(.*\)".*/\1/')

    if [[ -z "$CURRENT_VERSION" || -z "$CURRENT_VERSION_CODE" ]]; then
        log_error "Failed to parse current version from ${BUILD_GRADLE}"
        exit 1
    fi

    log_success "Current version: ${CURRENT_VERSION} (code: ${CURRENT_VERSION_CODE})"
}

calculate_new_version() {
    local bump_type="$1"

    # Parse semantic version components
    local major minor patch
    IFS='.' read -r major minor patch <<< "$CURRENT_VERSION"

    # Remove any non-numeric suffix (e.g., 1.9.1-beta -> 1.9.1)
    major="${major%%-*}"
    minor="${minor%%-*}"
    patch="${patch%%-*}"

    case "$bump_type" in
        major)
            NEW_VERSION="$((major + 1)).0.0"
            ;;
        minor)
            NEW_VERSION="${major}.$((minor + 1)).0"
            ;;
        patch)
            NEW_VERSION="${major}.${minor}.$((patch + 1))"
            ;;
        specified)
            NEW_VERSION="$SPECIFIED_VERSION"
            # Validate semantic versioning format
            if ! [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
                log_error "Invalid version format. Expected: MAJOR.MINOR.PATCH (e.g., 1.10.0)"
                log_error "Provided: $NEW_VERSION"
                exit 1
            fi
            ;;
        custom)
            read -p "Enter new version (current: ${CURRENT_VERSION}): " NEW_VERSION

            # Validate semantic versioning format
            if ! [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
                log_error "Invalid version format. Expected: MAJOR.MINOR.PATCH (e.g., 1.10.0)"
                exit 1
            fi
            ;;
        *)
            log_error "Invalid bump type: $bump_type"
            exit 1
            ;;
    esac

    # Auto-increment versionCode
    NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))
}

prompt_version_selection() {
    if [[ -n "$SPECIFIED_VERSION" ]]; then
        calculate_new_version "specified"
        return
    fi

    if [[ -n "$AUTO_BUMP" ]]; then
        calculate_new_version "$AUTO_BUMP"
        return
    fi

    log_header "Version Selection"

    echo "Current version: ${CURRENT_VERSION} (versionCode: ${CURRENT_VERSION_CODE})"
    echo ""
    echo "Select version bump type:"
    echo "[1] Major (${CURRENT_VERSION} → $(echo "$CURRENT_VERSION" | awk -F. '{print ($1+1)".0.0"}'))"
    echo "[2] Minor (${CURRENT_VERSION} → $(echo "$CURRENT_VERSION" | awk -F. '{print $1"."($2+1)".0"}'))"
    echo "[3] Patch (${CURRENT_VERSION} → $(echo "$CURRENT_VERSION" | awk -F. '{print $1"."$2"."($3+1)}'))"
    echo "[4] Custom (enter manually)"
    echo "[q] Quit"
    echo ""

    local choice
    read -p "Choice: " choice

    case "$choice" in
        1) calculate_new_version "major" ;;
        2) calculate_new_version "minor" ;;
        3) calculate_new_version "patch" ;;
        4) calculate_new_version "custom" ;;
        q|Q) log_info "Aborted by user"; exit 0 ;;
        *) log_error "Invalid choice"; exit 1 ;;
    esac

    log_success "Selected version: ${NEW_VERSION} (code: ${NEW_VERSION_CODE})"
}

confirm_version() {
    log_header "Release Summary"

    echo "Current: v${CURRENT_VERSION} (code: ${CURRENT_VERSION_CODE})"
    echo "New:     v${NEW_VERSION} (code: ${NEW_VERSION_CODE})"
    echo ""
    echo "This will:"
    echo "  - Update app/build.gradle.kts"
    echo "  - Update README.md badges"
    echo "  - Build signed APK"
    echo "  - Create git tag v${NEW_VERSION}"
    echo "  - Push to: $(IFS=', '; echo "${REMOTES[*]}")"
    echo ""

    if [[ "$DRY_RUN" == "true" ]]; then
        log_warning "DRY RUN MODE - No changes will be made"
        return
    fi

    if ! prompt_yes_no "Proceed with release?" "Y"; then
        log_info "Aborted by user"
        exit 0
    fi

    # Enable rollback from this point forward
    ROLLBACK_NEEDED=true
}

# ==============================================================================
# CHANGELOG GENERATION
# ==============================================================================

get_previous_tag() {
    log_info "Finding previous tag..."

    PREVIOUS_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")

    if [[ -z "$PREVIOUS_TAG" ]]; then
        log_warning "No previous tags found, using initial commit"
        PREVIOUS_TAG=$(git rev-list --max-parents=0 HEAD)
    else
        log_success "Previous tag: ${PREVIOUS_TAG}"
    fi
}

generate_changelog() {
    log_info "Generating changelog..."

    # Generate changelog from commits
    CHANGELOG=$(git log "${PREVIOUS_TAG}..HEAD" --oneline --no-merges --pretty=format:"- %s (%h)" || echo "")

    if [[ -z "$CHANGELOG" ]]; then
        log_warning "No commits found since ${PREVIOUS_TAG}"
        CHANGELOG="- No changes"
    fi

    # Save changelog to file
    mkdir -p "${BACKUP_DIR}"
    echo "$CHANGELOG" > "${BACKUP_DIR}/CHANGELOG_v${NEW_VERSION}.txt"

    log_success "Changelog generated"
}

preview_changelog() {
    log_header "Changelog Preview"

    echo "Changes since ${PREVIOUS_TAG}:"
    echo ""
    echo "$CHANGELOG"
    echo ""

    if [[ "$DRY_RUN" == "true" ]]; then
        return
    fi

    if ! prompt_yes_no "Changelog looks good?" "Y"; then
        log_info "Aborted by user"
        exit 0
    fi
}

# ==============================================================================
# FILE MODIFICATIONS
# ==============================================================================

create_backups() {
    log_info "Creating backups..."

    mkdir -p "${BACKUP_DIR}"
    cp "${BUILD_GRADLE}" "${BACKUP_DIR}/build.gradle.kts.bak"
    cp "${README}" "${BACKUP_DIR}/README.md.bak"

    log_success "Backups created in ${BACKUP_DIR}"
}

update_build_gradle() {
    log_info "Updating build.gradle.kts..."

    if [[ "$DRY_RUN" == "true" ]]; then
        log_warning "DRY RUN: Would update versionCode to ${NEW_VERSION_CODE}, versionName to ${NEW_VERSION}"
        return
    fi

    # Use awk for robust, line-independent updates
    awk -v code="${NEW_VERSION_CODE}" -v name="${NEW_VERSION}" '
    /^[[:space:]]*versionCode = / {
        sub(/versionCode = .*/, "versionCode = " code)
    }
    /^[[:space:]]*versionName = / {
        sub(/versionName = .*/, "versionName = \"" name "\"")
    }
    {print}
    ' "${BUILD_GRADLE}" > "${BUILD_GRADLE}.tmp" && mv "${BUILD_GRADLE}.tmp" "${BUILD_GRADLE}"

    log_success "build.gradle.kts updated"
}

update_readme() {
    log_info "Updating README.md badges..."

    if [[ "$DRY_RUN" == "true" ]]; then
        log_warning "DRY RUN: Would update version badges to v${NEW_VERSION}"
        return
    fi

    # Update version badges on lines 8-10
    sed -i "s/-v[0-9]\+\.[0-9]\+\.[0-9]\+\(-[^-]*\)\?-/-v${NEW_VERSION}-/g" "${README}"

    log_success "README.md updated"
}

verify_file_changes() {
    log_header "File Changes"

    echo "Changes to be committed:"
    echo ""

    # Show build.gradle.kts changes (most important)
    echo "build.gradle.kts:"
    if git diff --quiet "${BUILD_GRADLE}"; then
        echo "  (no changes)"
    else
        git diff "${BUILD_GRADLE}" | sed 's/^/  /'
    fi
    echo ""

    # Show README.md changes summary (avoid full diff)
    echo "README.md:"
    if git diff --quiet "${README}"; then
        echo "  (no changes)"
    else
        local readme_changes
        readme_changes=$(git diff --stat "${README}" | tail -1)
        echo "  ${readme_changes}"
        echo "  (version badges updated)"
    fi
    echo ""

    if [[ "$DRY_RUN" == "true" ]]; then
        return
    fi

    if ! prompt_yes_no "Confirm changes?" "Y"; then
        rollback "Changes rejected by user"
    fi
}

# ==============================================================================
# BUILD PROCESS
# ==============================================================================

clean_build() {
    log_info "Cleaning build directory..."

    if [[ "$DRY_RUN" == "true" ]]; then
        log_warning "DRY RUN: Would run ./gradlew clean"
        return
    fi

    # Create logs directory
    mkdir -p "${LOGS_DIR}"

    cd "${PROJECT_ROOT}"

    if [[ "$DEBUG" == "true" ]]; then
        ./gradlew clean || {
            rollback "Gradle clean failed"
        }
    else
        ./gradlew clean > "${LOGS_DIR}/clean_$(date +%Y%m%d_%H%M%S).log" 2>&1 || {
            cat "${LOGS_DIR}"/*.log | tail -20
            rollback "Gradle clean failed"
        }
    fi

    log_success "Build cleaned"
}

build_apk() {
    log_info "Building release APK (this may take a few minutes)..."

    if [[ "$DRY_RUN" == "true" ]]; then
        log_warning "DRY RUN: Would run ./gradlew assembleRelease"
        return
    fi

    cd "${PROJECT_ROOT}"

    # Build release APK with controlled output
    if [[ "$DEBUG" == "true" ]]; then
        if ! ./gradlew assembleRelease; then
            rollback "Gradle build failed"
        fi
    else
        # Show progress with a simple progress indicator
        echo -n "Building... "

        # Run build in background and monitor for completion
        local log_file="${LOGS_DIR}/build_$(date +%Y%m%d_%H%M%S).log"

        ./gradlew assembleRelease > "$log_file" 2>&1 &
        local build_pid=$!

        # Show simple progress dots while building
        while kill -0 $build_pid 2>/dev/null; do
            echo -n "."
            sleep 2
        done

        # Check if build succeeded
        if ! wait $build_pid; then
            echo " FAILED"
            echo ""
            echo "Last 20 lines of build log:"
            tail -20 "$log_file"
            rollback "Gradle build failed"
        fi

        echo " DONE"
    fi

    log_success "APK build completed"
}

verify_apk() {
    local apk_path="${PROJECT_ROOT}/app/build/outputs/apk/release/codex-v${NEW_VERSION}.apk"

    log_info "Verifying APK..."

    if [[ "$DRY_RUN" == "true" ]]; then
        log_warning "DRY RUN: Would verify APK at ${apk_path}"
        return
    fi

    if [[ ! -f "$apk_path" ]]; then
        rollback "APK not found at expected location: ${apk_path}"
    fi

    local apk_size
    apk_size=$(du -h "$apk_path" | cut -f1)

    log_success "APK verified: codex-v${NEW_VERSION}.apk (${apk_size})"
}

generate_checksum() {
    log_info "Generating SHA256 checksum..."

    if [[ "$DRY_RUN" == "true" ]]; then
        log_warning "DRY RUN: Would generate SHA256 checksum"
        return
    fi

    cd "${PROJECT_ROOT}/app/build/outputs/apk/release"

    local apk_file="codex-v${NEW_VERSION}.apk"
    local checksum_file="${apk_file}.sha256"

    sha256sum "$apk_file" > "$checksum_file"

    local checksum
    checksum=$(cat "$checksum_file" | cut -d' ' -f1)

    log_success "SHA256: ${checksum:0:16}..."

    cd "${PROJECT_ROOT}"
}

upload_to_gitlab_packages() {
    log_info "Uploading APK to GitLab Packages..."

    if [[ "$DRY_RUN" == "true" ]]; then
        log_warning "DRY RUN: Would upload APK to GitLab Packages"
        return
    fi

    # Check if GitLab token is available
    local gitlab_token="${GITLAB_TOKEN:-}"
    if [[ -z "$gitlab_token" ]]; then
        log_warning "GITLAB_TOKEN not set, skipping GitLab package upload"
        log_info "Set GITLAB_TOKEN environment variable to enable automatic uploads"
        return
    fi

    local apk_path="${PROJECT_ROOT}/app/build/outputs/apk/release/codex-v${NEW_VERSION}.apk"
    local package_url="https://gitlab.com/api/v4/projects/${GITLAB_PROJECT_ID:-YOUR_PROJECT_ID}/packages/generic/codex/${NEW_VERSION}/codex-v${NEW_VERSION}.apk"

    if curl --fail --silent --show-error \
           --header "PRIVATE-TOKEN: $gitlab_token" \
           --upload-file "$apk_path" \
           "$package_url"; then
        log_success "APK uploaded to GitLab Packages"
        # Store the download URL for use in release notes
        GITLAB_APK_URL="$package_url"
    else
        log_warning "Failed to upload APK to GitLab Packages"
        log_info "You can manually upload the APK later"
    fi
}

# ==============================================================================
# GIT OPERATIONS
# ==============================================================================

create_commit() {
    log_info "Creating git commit..."

    if [[ "$DRY_RUN" == "true" ]]; then
        log_warning "DRY RUN: Would commit changes"
        return
    fi

    git add "${BUILD_GRADLE}" "${README}"

    git commit -m "Release v${NEW_VERSION}

- Bump versionCode to ${NEW_VERSION_CODE}
- Bump versionName to ${NEW_VERSION}
- Update README version badges"

    COMMIT_CREATED=true

    log_success "Commit created"
}

create_tag() {
    log_info "Creating annotated tag..."

    if [[ "$DRY_RUN" == "true" ]]; then
        log_warning "DRY RUN: Would create tag v${NEW_VERSION}"
        return
    fi

    # Get SHA256 checksum for tag message
    local checksum=""
    local checksum_file="${PROJECT_ROOT}/app/build/outputs/apk/release/codex-v${NEW_VERSION}.apk.sha256"
    if [[ -f "$checksum_file" ]]; then
        checksum=$(cat "$checksum_file" | cut -d' ' -f1)
    fi

    # Create tag with changelog
    local tag_message="Release v${NEW_VERSION}

Changelog:
${CHANGELOG}

Signed APK: codex-v${NEW_VERSION}.apk"

    if [[ -n "$checksum" ]]; then
        tag_message="${tag_message}
SHA256: ${checksum}"
    fi

    git tag -a "v${NEW_VERSION}" -m "$tag_message"

    TAG_CREATED=true

    log_success "Tag v${NEW_VERSION} created"
}

push_to_remotes() {
    log_header "Pushing to Remotes"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_warning "DRY RUN: Would push to remotes"
        return
    fi

    local push_failed=false
    local failed_remotes=()

    for remote in "${REMOTES[@]}"; do
        if ! git remote get-url "${remote}" &>/dev/null; then
            log_warning "Skipping ${remote} (not configured)"
            continue
        fi

        log_info "Pushing to ${remote}..."

        # Try atomic push (both commit and tag)
        if git push --atomic "${remote}" master "v${NEW_VERSION}" 2>/dev/null; then
            log_success "${remote} updated"
        else
            # Fall back to sequential push
            if git push "${remote}" master && git push "${remote}" "v${NEW_VERSION}"; then
                log_success "${remote} updated"
            else
                log_error "Failed to push to ${remote}"
                push_failed=true
                failed_remotes+=("${remote}")
            fi
        fi
    done

    if [[ "$push_failed" == "true" ]]; then
        log_warning "Some remotes failed. You can manually retry with:"
        for remote in "${failed_remotes[@]}"; do
            echo "  git push ${remote} master && git push ${remote} v${NEW_VERSION}"
        done
    fi
}

verify_remotes() {
    if [[ "$DRY_RUN" == "true" ]]; then
        return
    fi

    log_info "Verifying tag on remotes..."

    for remote in "${REMOTES[@]}"; do
        if git remote get-url "${remote}" &>/dev/null; then
            if git ls-remote --tags "${remote}" | grep -q "refs/tags/v${NEW_VERSION}$"; then
                log_success "${remote} has tag v${NEW_VERSION}"
            fi
        fi
    done
}

# ==============================================================================
# COMPLETION SUMMARY
# ==============================================================================

show_completion_summary() {
    log_header "Release v${NEW_VERSION} Completed Successfully!"

    if [[ "$DRY_RUN" == "true" ]]; then
        echo "DRY RUN completed - no changes were made"
        echo ""
        echo "To perform the actual release, run without --dry-run flag"
        return
    fi

    local apk_path="${PROJECT_ROOT}/app/build/outputs/apk/release/codex-v${NEW_VERSION}.apk"
    local sha_path="${apk_path}.sha256"

    local commit_hash checksum=""
    commit_hash=$(git log -1 --format=%h)

    # Get checksum if available
    if [[ -f "$sha_path" ]]; then
        checksum=$(cat "$sha_path" | cut -d' ' -f1)
    fi

    echo ""
    echo "Git Status:"
    echo "  Commit: ${commit_hash} \"Release v${NEW_VERSION}\""
    echo "  Tag:    v${NEW_VERSION}"
    echo "  Pushed to: $(IFS=', '; echo "${REMOTES[*]}")"
    echo ""

    echo "New Release Links:"
    echo ""
    echo "GitHub:"
    echo "  https://github.com/BlindMint/codex/releases/new?tag=v${NEW_VERSION}"
    echo ""
    echo "GitLab:"
    echo "  https://gitlab.com/BlindMint/codex/-/releases/new?tag_name=v${NEW_VERSION}"
    if [[ -n "${GITLAB_APK_URL:-}" ]]; then
        echo "  APK Download: ${GITLAB_APK_URL}"
    fi
    echo ""
    echo "Codeberg:"
    echo "  https://codeberg.org/BlindMint/codex/releases/new?tag=v${NEW_VERSION}"
    echo ""

    echo "Copy/Paste Release Content:"
    echo ""
    echo "# Release v${NEW_VERSION}"
    echo ""
    echo "## Changelog"
    echo ""
    echo "$CHANGELOG"
    echo ""

    if [[ -n "${GITLAB_APK_URL:-}" ]]; then
        echo "## Downloads"
        echo ""
        echo "**APK:** [codex-v${NEW_VERSION}.apk](${GITLAB_APK_URL})"
        if [[ -n "$checksum" ]]; then
            echo "**SHA256:** \`$checksum\`"
        fi
        echo ""
    fi
    echo "Build Artifacts:"
    echo "  APK:    ${apk_path}"
    echo "  SHA256: ${sha_path}"
    echo ""
    echo "Upload both files:"
    echo "  - codex-v${NEW_VERSION}.apk"
    echo "  - codex-v${NEW_VERSION}.apk.sha256"
    echo ""

    log_success "Backups preserved in: ${BACKUP_DIR}"
}

# ==============================================================================
# MAIN EXECUTION
# ==============================================================================

parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --query|-q)
                QUERY_ONLY=true
                shift
                ;;
            --version|-v)
                if [[ $# -lt 2 ]] || [[ "${2:-}" == --* ]]; then
                    log_error "--version requires a version number (e.g., --version 2.0.1)"
                    exit 1
                fi
                SPECIFIED_VERSION="$2"
                shift 2
                ;;
            --dry-run)
                DRY_RUN=true
                log_warning "DRY RUN MODE ENABLED"
                shift
                ;;
            --force)
                FORCE=true
                shift
                ;;
            --major)
                AUTO_BUMP="major"
                shift
                ;;
            --minor)
                AUTO_BUMP="minor"
                shift
                ;;
            --patch)
                AUTO_BUMP="patch"
                shift
                ;;
            --debug|--verbose)
                DEBUG=true
                log_info "DEBUG MODE ENABLED - Detailed output will be shown"
                shift
                ;;
            -h|--help)
                echo "Codex Release Automation Script"
                echo ""
                echo "Usage: $0 [OPTIONS]"
                echo ""
                echo "Options:"
                echo "  -q, --query          Show current version and exit"
                echo "  -v, --version X.Y.Z  Release specific version (e.g., --version 2.0.1)"
                echo "  --dry-run            Preview changes without making them"
                echo "  --force              Skip git status check"
                echo "  --debug              Show detailed build output and logs"
                echo "  --major              Auto-select major version bump"
                echo "  --minor              Auto-select minor version bump"
                echo "  --patch              Auto-select patch version bump"
                echo "  -h, --help           Show this help message"
                echo ""
                echo "Examples:"
                echo "  $0                   # Interactive mode"
                echo "  $0 --query           # Show current version"
                echo "  $0 --version 2.0.1   # Release version 2.0.1"
                echo "  $0 --patch           # Auto-bump patch version"
                echo "  $0 --dry-run --minor # Preview minor version bump"
                echo ""
                echo "Environment Variables:"
                echo "  GITLAB_TOKEN=your_token         # Enable automatic APK upload to GitLab"
                echo "  GITLAB_PROJECT_ID=12345678      # Your GitLab project ID"
                echo ""
                echo "Or create a .env file (see .env.example for template)"
                echo ""
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                echo "Use --help for usage information"
                exit 1
                ;;
        esac
    done
}

main() {
    # Load environment variables from .env file if it exists
    if [[ -f ".env" ]]; then
        set -a  # automatically export all variables
        source ".env"
        set +a
        log_info ".env file loaded"
    fi

    parse_args "$@"

    # Handle query mode early
    if [[ "$QUERY_ONLY" == "true" ]]; then
        query_version
    fi

    log_header "Codex Release Automation"

    # Phase 1: Pre-flight checks
    check_working_directory
    check_git_status
    check_keystore
    check_gradle
    check_remotes

    # Phase 2: Version selection
    parse_current_version
    prompt_version_selection
    check_tag_exists
    confirm_version

    # Phase 3: Changelog
    get_previous_tag
    generate_changelog
    preview_changelog

    # Phase 4: File modifications
    create_backups
    update_build_gradle
    update_readme
    verify_file_changes

    # Phase 5: Build
    clean_build
    build_apk
    verify_apk
    generate_checksum
    upload_to_gitlab_packages

    # Phase 6: Git operations
    create_commit
    create_tag
    push_to_remotes
    verify_remotes

    # Phase 7: Completion
    show_completion_summary

    # Disable rollback (success)
    ROLLBACK_NEEDED=false
}

main "$@"
