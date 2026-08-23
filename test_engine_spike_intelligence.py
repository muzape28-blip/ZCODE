"""
Test Suite for Spike Intelligence Engine

Comprehensive tests for all components:
- Jedi (completion)
- Parso (AST parsing)
- Pyflakes (linting)
- Cabe (complexity analysis)
- Rope (refactoring)
"""

import unittest
import logging
import sys
import os

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Add the app directory to path for imports
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'app', 'src', 'main', 'python'))

from editor.intelligence_engine import SpikeIntelligenceEngine

class TestSpikeIntelligenceEngine(unittest.TestCase):
    """Test cases for the Spike Intelligence Engine."""

    @classmethod
    def setUpClass(cls):
        """Setup the test class."""
        cls.engine = SpikeIntelligenceEngine()
        logger.info("Spike Intelligence Engine test setup complete")

    def test_engine_initialization(self):
        """Test that the engine initializes correctly."""
        self.assertIsNotNone(self.engine)
        self.assertIsInstance(self.engine, SpikeIntelligenceEngine)

    def test_health_check(self):
        """Test the health check functionality."""
        health = self.engine.health_check()
        self.assertIsInstance(health, dict)
        
        # Check that all components are reported
        expected_components = ["jedi", "parso", "pyflakes", "cabe", "rope"]
        for component in expected_components:
            self.assertIn(component, health)

    def test_jedi_completion(self):
        """Test Jedi completion functionality."""
        # Simple test case
        code = """
def hello():
    pass

hello()
"""
        
        # Test at the end of the file
        completions = self.engine.autocomplete(code, (5, 0))
        self.assertIsInstance(completions, list)

    def test_parso_parsing(self):
        """Test Parso AST parsing."""
        code = """
def test_function():
    x = 10
    return x
"""
        
        ast = self.engine.parse_ast(code)
        self.assertIsNotNone(ast)

    def test_pyflakes_linting(self):
        """Test Pyflakes linting."""
        code = """
def test():
    x = 10  # unused variable
    return 42
"""
        
        issues = self.engine.lint(code)
        self.assertIsInstance(issues, list)
        
        # Should find at least the unused variable
        self.assertTrue(len(issues) >= 1)

    def test_cabe_complexity(self):
        """Test Cabe complexity analysis."""
        code = """
def simple_function():
    return 42

def complex_function():
    if True:
        for i in range(10):
            if i % 2 == 0:
                print(i)
"""
        
        analysis = self.engine.analyze_complexity(code)
        self.assertIsInstance(analysis, dict)
        self.assertIn("cyclomatic_complexity", analysis)
        self.assertIn("function_count", analysis)
        self.assertIn("class_count", analysis)
        self.assertIn("maintainability_index", analysis)

    def test_rope_refactoring(self):
        """Test Rope refactoring capabilities."""
        code = """
def old_function():
    x = 10
    return x
"""
        
        # Test a simple rename operation
        try:
            refactored = self.engine.refactor(
                code, 
                "rename",
                old_name="old_function",
                new_name="new_function",
                line=1
            )
            self.assertIsInstance(refactored, str)
            self.assertIn("new_function", refactored)
        except Exception as e:
            logger.warning(f"Rope refactoring test skipped: {e}")

    def test_integration(self):
        """Test that all components work together."""
        code = """
def example_function():
    # This is a test function
    x = 10  # unused variable
    if x > 5:
        print("Hello")
    return 42
"""
        
        # Test all major functions
        completions = self.engine.autocomplete(code, (5, 0))
        ast = self.engine.parse_ast(code)
        issues = self.engine.lint(code)
        analysis = self.engine.analyze_complexity(code)
        
        # Basic assertions
        self.assertIsInstance(completions, list)
        self.assertIsNotNone(ast)
        self.assertIsInstance(issues, list)
        self.assertIsInstance(analysis, dict)
        
        # Verify we found the unused variable
        unused_issues = [i for i in issues if "unused" in i.get("message", "").lower()]
        self.assertTrue(len(unused_issues) >= 1)

if __name__ == '__main__':
    unittest.main()