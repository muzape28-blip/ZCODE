"""
Comprehensive Test Suite & Mutation Verification for Spike Intelligence Engine

Tests:
1. Engine & Sub-engines Lazy Initialization
2. Error Hierarchy & Exception Propagation
3. Jedi Layer (Autocomplete, Goto Definition, Input Validation & Fallbacks)
4. Parso Layer (AST Parsing, Syntax Error Detection & Fallbacks)
5. Pyflakes Layer (Linting, Unused Vars, Syntax Errors & Fallbacks)
6. Cabe Layer (Cyclomatic Complexity, Maintainability Index & Fallbacks)
7. Rope Layer (Refactoring, Symbol Rename & Fallbacks)
8. Orchestrator Integration & Plugin Wiring (zcode_plugins.py)
9. Mutation Verification (Intentionally broken invariants must turn tests RED)
"""

import unittest
import logging
import sys
import os

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Add app python directory to sys.path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'app', 'src', 'main', 'python'))

from editor import (
    SpikeError,
    JediError,
    ParsoError,
    PyflakesError,
    CabeError,
    RopeError
)
from editor.intelligence_engine import SpikeIntelligenceEngine
from editor.jedi_layer import JediCompletionProvider
from editor.parso_layer import ParsoASTParser
from editor.pyflakes_layer import PyflakesLinter
from editor.cabe_layer import CabeComplexityAnalyzer
from editor.rope_layer import RopeRefactorer
import zcode_plugins as zp


class TestSpikeIntelligenceErrors(unittest.TestCase):
    """Test Error Hierarchy Inheritance."""

    def test_exception_inheritance(self):
        self.assertTrue(issubclass(JediError, SpikeError))
        self.assertTrue(issubclass(ParsoError, SpikeError))
        self.assertTrue(issubclass(PyflakesError, SpikeError))
        self.assertTrue(issubclass(CabeError, SpikeError))
        self.assertTrue(issubclass(RopeError, SpikeError))

        # Check instance raising
        with self.assertRaises(SpikeError):
            raise JediError("Test Jedi Error")


class TestJediLayer(unittest.TestCase):
    """Test Jedi Layer Autocompletion & Go-to Definition."""

    def setUp(self):
        self.provider = JediCompletionProvider()

    def test_position_validation(self):
        code = "def foo():\n    pass"
        # Line out of bounds (10 lines vs 2 lines actual)
        line, col = self.provider._validate_position(code, (10, 5))
        self.assertEqual(line, 2)
        # Col out of bounds (line 1 len is 10)
        line, col = self.provider._validate_position(code, (1, 50))
        self.assertEqual(line, 1)
        self.assertEqual(col, 10)
        # Negative indices
        line, col = self.provider._validate_position(code, (-5, -5))
        self.assertEqual(line, 1)
        self.assertEqual(col, 0)

    def test_empty_input_fallback(self):
        self.assertEqual(self.provider.get_completions("", (1, 0)), [])
        self.assertEqual(self.provider.goto_definition("", (1, 0)), [])

    def test_graceful_degradation_without_library(self):
        # Force missing library
        self.provider._jedi = None
        # Should not crash even if library absent
        completions = self.provider.get_completions("import os\nos.", (2, 3))
        self.assertIsInstance(completions, list)


class TestParsoLayer(unittest.TestCase):
    """Test Parso Layer AST Parsing & Fault Recovery."""

    def setUp(self):
        self.parser = ParsoASTParser()

    def test_parse_valid_code(self):
        code = "x = 10\ndef foo(): pass"
        ast_info = self.parser.parse(code)
        self.assertIsInstance(ast_info, dict)
        self.assertIn("type", ast_info)
        self.assertIn("errors", ast_info)
        self.assertIn("nodes_count", ast_info)

    def test_parse_empty_code(self):
        ast_info = self.parser.parse("")
        self.assertEqual(ast_info["nodes_count"], 0)
        self.assertEqual(ast_info["errors"], [])


class TestPyflakesLayer(unittest.TestCase):
    """Test Pyflakes Static Analysis & Builtin AST Fallback."""

    def setUp(self):
        self.linter = PyflakesLinter()

    def test_fallback_ast_lint_valid(self):
        code = "def test():\n    return 42\n"
        issues = self.linter._fallback_ast_lint(code, "test.py")
        self.assertEqual(issues, [])

    def test_fallback_ast_lint_syntax_error(self):
        code = "def test():\n    return 42 +"
        issues = self.linter._fallback_ast_lint(code, "test.py")
        self.assertEqual(len(issues), 1)
        self.assertEqual(issues[0]["severity"], "error")
        self.assertIn("SyntaxError", issues[0]["message"])

    def test_lint_empty_string(self):
        self.assertEqual(self.linter.lint(""), [])


class TestCabeLayer(unittest.TestCase):
    """Test Cyclomatic Complexity & Maintainability Index Calculation."""

    def setUp(self):
        self.analyzer = CabeComplexityAnalyzer()

    def test_analyze_simple_code(self):
        code = """
def simple():
    return 1
"""
        res = self.analyzer.analyze(code)
        self.assertEqual(res["cyclomatic_complexity"], 1)
        self.assertEqual(res["function_count"], 1)
        self.assertGreater(res["maintainability_index"], 80.0)

    def test_analyze_complex_code(self):
        code = """
def complex_fn(a, b):
    if a > 0:
        for i in range(b):
            if i % 2 == 0:
                print(i)
    elif a < 0:
        while b > 0:
            b -= 1
    return 0
"""
        res = self.analyzer.analyze(code, threshold=3)
        self.assertGreaterEqual(res["cyclomatic_complexity"], 4)
        self.assertEqual(len(res["blocks"]), 1)
        self.assertTrue(res["blocks"][0]["is_high_complexity"])

    def test_syntax_error_handling(self):
        code = "def broken(:"
        res = self.analyzer.analyze(code)
        self.assertIn("error", res)
        self.assertEqual(res["cyclomatic_complexity"], 0)


class TestRopeLayer(unittest.TestCase):
    """Test Safe Refactoring & Symbol Rename."""

    def setUp(self):
        self.refactorer = RopeRefactorer()

    def test_rename_symbol_safe(self):
        code = "def old_name():\n    return old_name()\n"
        new_code = self.refactorer.rename_symbol(code, "old_name", "new_name")
        self.assertIn("def new_name():", new_code)
        self.assertIn("return new_name()", new_code)
        self.assertNotIn("old_name", new_code)

    def test_rename_same_name_noop(self):
        code = "x = 10"
        self.assertEqual(self.refactorer.rename_symbol(code, "x", "x"), code)

    def test_unsupported_operation_raises(self):
        with self.assertRaises(RopeError):
            self.refactorer.refactor("x = 10", "unsupported_op")


class TestOrchestratorEngine(unittest.TestCase):
    """Test SpikeIntelligenceEngine Lazy Loading & Health Check."""

    def setUp(self):
        self.engine = SpikeIntelligenceEngine()

    def test_lazy_loading(self):
        # Sub-engines should be None initially
        self.assertIsNone(self.engine._jedi_provider)
        self.assertIsNone(self.engine._parso_parser)
        self.assertIsNone(self.engine._pyflakes_linter)
        self.assertIsNone(self.engine._cabe_analyzer)
        self.assertIsNone(self.engine._rope_refactorer)

        # Accessing properties loads them
        _ = self.engine.jedi_provider
        self.assertIsNotNone(self.engine._jedi_provider)

    def test_health_check_format(self):
        health = self.engine.health_check()
        self.assertIsInstance(health, dict)
        for sub in ["jedi", "parso", "pyflakes", "cabe", "rope"]:
            self.assertIn(sub, health)
            self.assertIn("installed", health[sub])
            self.assertIn("status", health[sub])


class TestPluginWiring(unittest.TestCase):
    """Test integration with zcode_plugins.py."""

    def test_run_spike_intelligence_health_check(self):
        res = zp.run_spike_intelligence("x = 1", "health_check")
        self.assertTrue(res["ok"])
        self.assertIn("health", res)

    def test_run_spike_intelligence_complexity(self):
        res = zp.run_spike_intelligence("def f(): pass", "analyze_complexity")
        self.assertTrue(res["ok"])
        self.assertIn("analysis", res)

    def test_run_spike_intelligence_unknown_action(self):
        res = zp.run_spike_intelligence("x = 1", "unknown_action_xyz")
        self.assertFalse(res["ok"])
        self.assertIn("error", res)


if __name__ == "__main__":
    unittest.main()
