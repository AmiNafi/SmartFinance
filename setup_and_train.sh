#!/bin/bash

# Complete MobileBERT Training Setup and Execution Script
# This script handles the entire ML pipeline for SmartFinance

set -e  # Exit on any error

echo "🚀 SmartFinance MobileBERT Training Pipeline"
echo "=========================================="

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

# Check if Python 3 is available
check_python() {
    print_status "Checking Python installation..."
    if ! command -v python3 &> /dev/null; then
        print_error "Python 3 is not installed. Please install Python 3.7+ first."
        exit 1
    fi

    PYTHON_VERSION=$(python3 --version | cut -d' ' -f2)
    print_success "Python $PYTHON_VERSION found"
}

# Create virtual environment
setup_venv() {
    print_status "Setting up Python virtual environment..."

    if [ ! -d "ml_env" ]; then
        python3 -m venv ml_env
        print_success "Virtual environment created"
    else
        print_warning "Virtual environment already exists"
    fi

    # Activate virtual environment
    source ml_env/bin/activate
    print_success "Virtual environment activated"
}

# Install required packages
install_dependencies() {
    print_status "Installing ML dependencies..."
    pip install --upgrade pip

    # Install PyTorch (CPU version for Mac)
    pip install torch torchvision torchaudio

    # Install TensorFlow
    pip install tensorflow tensorflow-hub tensorflow-text

    # Install Transformers
    pip install transformers

    # Install other dependencies
    pip install scikit-learn pandas numpy tqdm

    print_success "All dependencies installed"
}

# Verify installations
verify_installations() {
    print_status "Verifying installations..."

    python3 -c "import torch; print(f'PyTorch: {torch.__version__}')"
    python3 -c "import tensorflow as tf; print(f'TensorFlow: {tf.__version__}')"
    python3 -c "import transformers; print(f'Transformers: {transformers.__version__}')"
    python3 -c "import sklearn; print(f'scikit-learn: {sklearn.__version__}')"

    print_success "All installations verified"
}

# Run the training script
run_training() {
    print_status "Starting MobileBERT training..."

    if [ ! -f "train_mobilebert.py" ]; then
        print_error "Training script not found!"
        exit 1
    fi

    python3 train_mobilebert.py
}

# Check if model was created successfully
verify_output() {
    print_status "Verifying training output..."

    if [ -f "app/src/main/assets/mobilebert_transaction_classifier.tflite" ]; then
        MODEL_SIZE=$(stat -f%z "app/src/main/assets/mobilebert_transaction_classifier.tflite" 2>/dev/null || stat -c%s "app/src/main/assets/mobilebert_transaction_classifier.tflite" 2>/dev/null)
        MODEL_SIZE_MB=$((MODEL_SIZE / 1024 / 1024))
        print_success "TFLite model created (${MODEL_SIZE_MB}MB)"
    else
        print_error "TFLite model not found!"
        exit 1
    fi

    if [ -f "app/src/main/assets/model_config.json" ]; then
        print_success "Model configuration created"
    else
        print_error "Model configuration not found!"
        exit 1
    fi

    if [ -f "transaction_dataset.csv" ]; then
        print_success "Training dataset saved"
    else
        print_warning "Training dataset not found"
    fi
}

# Main execution
main() {
    print_status "Starting complete MobileBERT training pipeline..."

    # Step 1: Check Python
    check_python

    # Step 2: Setup virtual environment
    setup_venv

    # Step 3: Install dependencies
    install_dependencies

    # Step 4: Verify installations
    verify_installations

    # Step 5: Run training
    run_training

    # Step 6: Verify output
    verify_output

    print_success "🎉 MobileBERT training pipeline completed successfully!"
    print_success "Your SmartFinance app now has a trained ML model for transaction classification!"
    echo ""
    print_status "Next steps:"
    echo "  1. Build and run your Android app"
    echo "  2. Test the AI transaction detection"
    echo "  3. Monitor performance and accuracy"
    echo "  4. Fine-tune the model if needed"
}

# Handle command line arguments
case "${1:-}" in
    "setup-only")
        check_python
        setup_venv
        install_dependencies
        verify_installations
        print_success "Setup completed. Run './setup_and_train.sh' to start training."
        ;;
    "train-only")
        if [ ! -d "ml_env" ]; then
            print_error "Virtual environment not found. Run './setup_and_train.sh' first."
            exit 1
        fi
        source ml_env/bin/activate
        run_training
        verify_output
        ;;
    "verify")
        verify_output
        ;;
    *)
        main
        ;;
esac
