#!/bin/bash

# =============================================================================
# Docker Build Script for FHIR Bridge
# Supports multiple build targets and environments
# =============================================================================

set -e

# Default values
BUILD_TARGET="production"
IMAGE_TAG="latest"
PUSH_IMAGE=false
REGISTRY=""
BUILD_ARGS=""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Build Docker images for FHIR Bridge application

OPTIONS:
    -t, --target TARGET     Build target (production, development) [default: production]
    -i, --image TAG         Image tag [default: latest]
    -p, --push              Push image to registry after build
    -r, --registry URL      Registry URL for pushing
    -a, --build-arg ARG     Additional build arguments (can be used multiple times)
    -h, --help              Show this help message

EXAMPLES:
    # Build production image
    $0 --target production --image fhir-bridge:1.0.0

    # Build development image
    $0 --target development --image fhir-bridge:dev

    # Build and push to registry
    $0 --target production --image fhir-bridge:1.0.0 --push --registry myregistry.com

    # Build with custom build arguments
    $0 --build-arg MAVEN_OPTS="-Xmx1024m" --build-arg SKIP_TESTS=true

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--target)
            BUILD_TARGET="$2"
            shift 2
            ;;
        -i|--image)
            IMAGE_TAG="$2"
            shift 2
            ;;
        -p|--push)
            PUSH_IMAGE=true
            shift
            ;;
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -a|--build-arg)
            BUILD_ARGS="$BUILD_ARGS --build-arg $2"
            shift 2
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Validate build target
if [[ "$BUILD_TARGET" != "production" && "$BUILD_TARGET" != "development" ]]; then
    print_error "Invalid build target: $BUILD_TARGET. Must be 'production' or 'development'"
    exit 1
fi

# Set Dockerfile based on target
if [[ "$BUILD_TARGET" == "development" ]]; then
    DOCKERFILE="Dockerfile.dev"
else
    DOCKERFILE="Dockerfile"
fi

# Construct full image name
if [[ -n "$REGISTRY" ]]; then
    FULL_IMAGE_NAME="$REGISTRY/$IMAGE_TAG"
else
    FULL_IMAGE_NAME="$IMAGE_TAG"
fi

print_status "Starting Docker build..."
print_status "Build target: $BUILD_TARGET"
print_status "Dockerfile: $DOCKERFILE"
print_status "Image name: $FULL_IMAGE_NAME"

# Check if Dockerfile exists
if [[ ! -f "$DOCKERFILE" ]]; then
    print_error "Dockerfile not found: $DOCKERFILE"
    exit 1
fi

# Build the image
print_status "Building Docker image..."
if docker build -f "$DOCKERFILE" -t "$FULL_IMAGE_NAME" $BUILD_ARGS .; then
    print_success "Docker image built successfully: $FULL_IMAGE_NAME"
else
    print_error "Docker build failed"
    exit 1
fi

# Show image information
print_status "Image information:"
docker images "$FULL_IMAGE_NAME" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"

# Push image if requested
if [[ "$PUSH_IMAGE" == true ]]; then
    if [[ -z "$REGISTRY" ]]; then
        print_warning "Registry not specified, skipping push"
    else
        print_status "Pushing image to registry..."
        if docker push "$FULL_IMAGE_NAME"; then
            print_success "Image pushed successfully: $FULL_IMAGE_NAME"
        else
            print_error "Failed to push image"
            exit 1
        fi
    fi
fi

# Run basic validation
print_status "Running basic validation..."
if docker run --rm "$FULL_IMAGE_NAME" java -version > /dev/null 2>&1; then
    print_success "Image validation passed"
else
    print_error "Image validation failed"
    exit 1
fi

print_success "Build process completed successfully!"

# Show next steps
cat << EOF

${GREEN}Next Steps:${NC}
1. Test the image locally:
   docker run --rm -p 8080:8080 -p 8081:8081 $FULL_IMAGE_NAME

2. Run with docker-compose:
   docker-compose up

3. Check application health:
   curl http://localhost:8081/actuator/health

EOF