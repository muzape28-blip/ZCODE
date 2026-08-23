"""
Test Suite for Spike Intelligence Engine

Comprehensive tests for all components with lazy loading, graceful degradation,
caching, and error handling.
"""

import unittest
import logging
import sys
import os
import tempfile
import threading
from typing import List, Dict, Any, Tuple

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Add the app directory to path for imports
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'app', 'src', 'main', 'python'))

from editor.intelligence_engine import SpikeIntelligenceEngine
from editor.jedi_layer import JediWrapper, DummyJediWrapper
from editor.parso_layer import ParsoWrapper, DummyParsoWrapper
from editor.pyflakes_layer import PyflakesWrapper, DummyPyflakesWrapper
from editor.cabe_layer import CabeWrapper, DummyCabeWrapper
from editor.rope_layer import RopeWrapper, DummyRopeWrapper

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

    def test_lazy_loading(self):
        """Test that components are lazy-loaded."""
        # Check that components are not loaded yet
        self.assertIsNone(self.engine._jedi_provider)
        self.assertIsNone(self.engine._parso_parser)
        self.assertIsNone(self.engine._pyflakes_linter)
        self.assertIsNone(self.engine._cabe_analyzer)
        self.assertIsNone(self.engine._rope_refactorer)
        
        # Trigger lazy loading
        _ = self.engine.jedi_provider
        _ = self.engine.parso_parser
        _ = self.engine.pyflakes_linter
        _ = self.engine.cabe_analyzer
        _ = self.engine.rope_refactorer
        
        # Check that components are now loaded
        self.assertIsNotNone(self.engine._jedi_provider)
        self.assertIsNotNone(self.engine._parso_parser)
        self.assertIsNotNone(self.engine._pyflakes_linter)
        self.assertIsNotNone(self.engine._cabe_analyzer)
        self.assertIsNotNone(self.engine._rope_refactorer)

    def test_graceful_degradation(self):
        """Test graceful degradation when components are not available."""
        # Create a mock engine with dummy components
        class MockEngine(SpikeIntelligenceEngine):
            @property
            def jedi_provider(self):
                return DummyJediWrapper()
            
            @property
            def parso_parser(self):
                return DummyParsoWrapper()
            
            @property
            def pyflakes_linter(self):
                return DummyPyflakesWrapper()
            
            @property
            def cabe_analyzer(self):
                return DummyCabeWrapper()
            
            @property
            def rope_refactorer(self):
                return DummyRopeWrapper()

        mock_engine = MockEngine()
        
        # Test that dummy components are used
        self.assertIsInstance(mock_engine.jedi_provider, DummyJediWrapper)
        self.assertIsInstance(mock_engine.parso_parser, DummyParsoWrapper)
        self.assertIsInstance(mock_engine.pyflakes_linter, DummyPyflakesWrapper)
        self.assertIsInstance(mock_engine.cabe_analyzer, DummyCabeWrapper)
        self.assertIsInstance(mock_engine.rope_refactorer, DummyRopeWrapper)
        
        # Test that operations still work with dummy components
        completions = mock_engine.autocomplete("def x():", (2, 0))
        self.assertIsInstance(completions, list)
        
        ast = mock_engine.parse_ast("def x():")
        self.assertIsNone(ast)
        
        issues = mock_engine.lint("def x():")
        self.assertIsInstance(issues, list)
        
        analysis = mock_engine.analyze_complexity("def x():")
        self.assertIsInstance(analysis, dict)
        
        refactored = mock_engine.refactor("def x():", "rename", old_name="x", new_name="y", line=1)
        self.assertIsInstance(refactored, str)

    def test_caching(self):
        """Test that caching works for expensive operations."""
        code = """
def test_function():
    x = 10
    return x
"""
        
        # Test Jedi caching
        completions1 = self.engine.autocomplete(code, (3, 4))
        completions2 = self.engine.autocomplete(code, (3, 4))
        self.assertEqual(completions1, completions2)
        
        # Test Parso caching
        ast1 = self.engine.parse_ast(code)
        ast2 = self.engine.parse_ast(code)
        self.assertEqual(ast1, ast2)

    def test_error_handling(self):
        """Test error handling for all operations."""
        # Test Jedi error handling
        completions = self.engine.autocomplete("invalid code", (1, 1))
        self.assertIsInstance(completions, list)
        
        # Test Parso error handling
        ast = self.engine.parse_ast("invalid code")
        self.assertIsNone(ast)
        
        # Test Pyflakes error handling
        issues = self.engine.lint("invalid code")
        self.assertIsInstance(issues, list)
        
        # Test Cabe error handling
        analysis = self.engine.analyze_complexity("invalid code")
        self.assertIsInstance(analysis, dict)
        
        # Test Rope error handling
        refactored = self.engine.refactor("invalid code", "rename", old_name="x", new_name="y", line=1)
        self.assertIsInstance(refactored, str)

    def test_jedi_completion(self):
        """Test Jedi completion functionality."""
        code = """
def hello():
    pass

hello()
"""
        
        # Test at the end of the file
        completions = self.engine.autocomplete(code, (5, 0))
        self.assertIsInstance(completions, list)
        self.assertTrue(len(completions) > 0)

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
        refactored = self.engine.refactor(
            code,
            "rename",
            old_name="old_function",
            new_name="new_function",
            line=1
        )
        self.assertIsInstance(refactored, str)
        self.assertIn("new_function", refactored)

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

    def test_background_thread(self):
        """Test that expensive operations can be run in background threads."""
        code = """
def complex_function():
    if True:
        for i in range(100):
            if i % 2 == 0:
                print(i)
"""
        
        # Run analysis in background thread
        result = None
        
        def analyze():
            nonlocal result
            result = self.engine.analyze_complexity(code)

        thread = threading.Thread(target=analyze)
        thread.start()
        thread.join(timeout=5)
        
        self.assertIsNotNone(result)
        self.assertIsInstance(result, dict)
        self.assertIn("cyclomatic_complexity", result)

if __name__ == '__main__':
    unittest.main()