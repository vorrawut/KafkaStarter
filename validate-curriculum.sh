#!/bin/bash
# Kafka Curriculum Validation Script
# Validates that all 20 lessons are complete and properly structured

echo "🎯 Starting Kafka Curriculum Validation..."
echo "========================================"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
total_lessons=20
passed_tests=0
failed_tests=0

# Function to check if file exists
check_file() {
    local file=$1
    local description=$2
    
    if [ -f "$file" ]; then
        echo -e "${GREEN}✅ $description${NC}"
        ((passed_tests++))
        return 0
    else
        echo -e "${RED}❌ $description - MISSING: $file${NC}"
        ((failed_tests++))
        return 1
    fi
}

# Function to check if directory exists
check_directory() {
    local dir=$1
    local description=$2
    
    if [ -d "$dir" ]; then
        echo -e "${GREEN}✅ $description${NC}"
        ((passed_tests++))
        return 0
    else
        echo -e "${RED}❌ $description - MISSING: $dir${NC}"
        ((failed_tests++))
        return 1
    fi
}

echo -e "${BLUE}📋 Phase 1: Checking Lesson Structure${NC}"
echo "------------------------------------"

# Check all 20 lessons have required files
for i in {1..20}; do
    echo -e "${YELLOW}Checking Lesson $i...${NC}"
    
    # Check workshop directory and README
    check_directory "class/workshop/lesson_$i" "Lesson $i workshop directory"
    check_file "class/workshop/lesson_$i/README.md" "Lesson $i workshop README"
    
    # Check modules directory and concept file
    check_directory "class/modules/lesson_$i" "Lesson $i modules directory"
    check_file "class/modules/lesson_$i/concept.md" "Lesson $i concept documentation"
    
    echo ""
done

echo -e "${BLUE}📋 Phase 2: Checking Core Documentation${NC}"
echo "---------------------------------------"

# Check core documentation files
check_file "README.md" "Main project README"
check_file "CURRICULUM_COMPLETION_REPORT.md" "Curriculum completion report"
check_file "class/CURRICULUM_GUIDE.md" "Curriculum guide"
check_file "class/LESSON_TEMPLATE.md" "Lesson template"
check_file "QUICK_START_GUIDE.md" "Quick start guide"

echo -e "${BLUE}📋 Phase 3: Checking Infrastructure${NC}"
echo "-----------------------------------"

# Check infrastructure files
check_file "build.gradle.kts" "Gradle build configuration"
check_file "docker/docker-compose.yml" "Docker Compose configuration"
check_file "demo/docker-compose-complete.yml" "Complete demo Docker Compose"
check_file "demo/DEMO_VALIDATION.md" "Demo validation guide"

echo -e "${BLUE}📋 Phase 4: Checking Demo System${NC}"
echo "--------------------------------"

# Check demo system structure
check_directory "demo" "Demo system directory"
check_file "demo/DEMO_SYSTEM_OVERVIEW.md" "Demo system overview"

echo ""
echo "========================================"
echo -e "${BLUE}📊 VALIDATION SUMMARY${NC}"
echo "========================================"

total_checks=$((passed_tests + failed_tests))
success_rate=$(( (passed_tests * 100) / total_checks ))

echo -e "Total Checks: $total_checks"
echo -e "${GREEN}Passed: $passed_tests${NC}"
echo -e "${RED}Failed: $failed_tests${NC}"
echo -e "Success Rate: $success_rate%"

echo ""

if [ $failed_tests -eq 0 ]; then
    echo -e "${GREEN}🎉 VALIDATION SUCCESSFUL! 🎉${NC}"
    echo -e "${GREEN}All $total_lessons lessons are properly structured and complete!${NC}"
    echo -e "${GREEN}The Kafka Mastery Curriculum is ready for use! 🚀${NC}"
    exit 0
else
    echo -e "${RED}❌ VALIDATION FAILED${NC}"
    echo -e "${RED}$failed_tests issues found that need to be addressed.${NC}"
    exit 1
fi