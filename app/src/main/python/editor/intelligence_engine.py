"""
Spike Intelligence Engine Core

Modul inti yang mengintegrasikan:
- Jedi: Autocompletion (lazy-loaded)
- Parso: AST Parsing (lazy-loaded)
- Pyflakes: Linting (lazy-loaded)
- Cabe: Complexity Analysis (lazy-loaded)
- Rope: Refactoring (lazy-loaded)

Fitur:
- Lazy loading untuk efisiensi
- Graceful degradation jika library tidak tersedia
- Caching untuk performa
- Error handling komprehensif
"""

import logging
from functools import lru_cache
from typing import Dict, Any, Optional, List, Tuple

# Setup logging
logger = logging.getLogger(__name__)

class SpikeIntelligenceEngine:
    """Main intelligence engine with lazy loading and graceful degradation."""

    def __init__(self):
        """Initialize all sub-engines with lazy loading."""
        self._jedi_provider = None
        self._parso_parser = None
        self._pyflakes_linter = None
        self._cabe_analyzer = None
        self._rope_refactorer = None
        logger.info("Spike Intelligence Engine initialized (lazy load)")

    # --- Lazy Loading Properties ---
    @property
    def jedi_provider(self):
        """Lazy load Jedi provider with graceful degradation."""
        if self._jedi_provider is None:
            try:
                import jedi
                self._jedi_provider = JediWrapper(jedi)
                logger.info("Jedi initialized successfully")
            except ImportError as e:
                logger.warning(f"Jedi not available: {e}")
                self._jedi_provider = DummyJediWrapper()
        return self._jedi_provider

    @property
    def parso_parser(self):
        """Lazy load Parso parser with graceful degradation."""
        if self._parso_parser is None:
            try:
                import parso
                self._parso_parser = ParsoWrapper(parso)
                logger.info("Parso initialized successfully")
            except ImportError as e:
                logger.warning(f"Parso not available: {e}")
                self._parso_parser = DummyParsoWrapper()
        return self._parso_parser

    @property
    def pyflakes_linter(self):
        """Lazy load Pyflakes linter with graceful degradation."""
        if self._pyflakes_linter is None:
            try:
                import pyflakes.api
                self._pyflakes_linter = PyflakesWrapper(pyflakes.api)
                logger.info("Pyflakes initialized successfully")
            except ImportError as e:
                logger.warning(f"Pyflakes not available: {e}")
                self._pyflakes_linter = DummyPyflakesWrapper()
        return self._pyflakes_linter

    @property
    def cabe_analyzer(self):
        """Lazy load Cabe analyzer with graceful degradation."""
        if self._cabe_analyzer is None:
            try:
                import mccabe
                self._cabe_analyzer = CabeWrapper()
                logger.info("Cabe initialized successfully")
            except ImportError as e:
                logger.warning(f"Cabe not available: {e}")
                self._cabe_analyzer = DummyCabeWrapper()
        return self._cabe_analyzer

    @property
    def rope_refactorer(self):
        """Lazy load Rope refactorer with graceful degradation."""
        if self._rope_refactorer is None:
            try:
                import rope.base.project
                self._rope_refactorer = RopeWrapper(rope)
                logger.info("Rope initialized successfully")
            except ImportError as e:
                logger.warning(f"Rope not available: {e}")
                self._rope_refactorer = DummyRopeWrapper()
        return self._rope_refactorer

    # --- Public API ---
    def autocomplete(self, code: str, position: Tuple[int, int]) -> List[Dict[str, Any]]:
        """Get code completions using Jedi."""
        return self.jedi_provider.get_completions(code, position)

    def parse_ast(self, code: str) -> Optional[Any]:
        """Parse code into AST using Parso."""
        return self.parso_parser.parse(code)

    def lint(self, code: str) -> List[Dict[str, Any]]:
        """Run static analysis using Pyflakes."""
        return self.pyflakes_linter.lint(code)

    def analyze_complexity(self, code: str) -> Dict[str, Any]:
        """Analyze code complexity using Cabe."""
        return self.cabe_analyzer.analyze(code)

    def refactor(self, code: str, operation: str, **kwargs) -> str:
        """Perform code refactoring using Rope."""
        return self.rope_refactorer.refactor(code, operation, **kwargs)

    def health_check(self) -> Dict[str, bool]:
        """Check if all sub-engines are operational."""
        return {
            "jedi": not isinstance(self.jedi_provider, DummyJediWrapper),
            "parso": not isinstance(self.parso_parser, DummyParsoWrapper),
            "pyflakes": not isinstance(self.pyflakes_linter, DummyPyflakesWrapper),
            "cabe": not isinstance(self.cabe_analyzer, DummyCabeWrapper),
            "rope": not isinstance(self.rope_refactorer, DummyRopeWrapper),
        }

# --- Wrapper Classes (Lazy Loading + Caching) ---
class JediWrapper:
    """Wrapper for Jedi with caching and error handling."""

    def __init__(self, jedi):
        self._jedi = jedi

    @lru_cache(maxsize=100)
    def get_completions(self, code: str, position: Tuple[int, int]) -> List[Dict[str, Any]]:
        """Get completions with caching."""
        try:
            script = self._jedi.Script(code)
            completions = script.complete(*position)
            return [{
                "name": c.name,
                "type": c.type,
                "description": c.docstring() if c.docstring() else ""
            } for c in completions]
        except Exception as e:
            logger.error(f"Jedi completion error: {e}")
            return []

    def get_definitions(self, code: str, position: Tuple[int, int]) -> List[Dict[str, Any]]:
        """Get definitions for symbol at position."""
        try:
            script = self._jedi.Script(code)
            definitions = script.goto(*position)
            return [{
                "name": d.name,
                "type": d.type,
                "module": d.module_name,
                "line": d.line,
                "column": d.column
            } for d in definitions]
        except Exception as e:
            logger.error(f"Jedi definition error: {e}")
            return []

class DummyJediWrapper:
    """Fallback if Jedi is not available."""

    def get_completions(self, code: str, position: Tuple[int, int]) -> List[Dict[str, Any]]:
        logger.warning("Jedi not available - using dummy completions")
        return []

    def get_definitions(self, code: str, position: Tuple[int, int]) -> List[Dict[str, Any]]:
        logger.warning("Jedi not available - using dummy definitions")
        return []