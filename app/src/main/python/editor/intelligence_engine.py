"""
Spike Intelligence Engine Core

Modul inti yang mengintegrasikan:
- Jedi: Autocompletion
- Parso: AST Parsing
- Pyflakes8: Linting
- Cabe: Complexity Analysis
- Rope: Refactoring
"""

import logging
from typing import Dict, Any, Optional, List, Tuple

# Setup logging
logger = logging.getLogger(__name__)

class SpikeIntelligenceEngine:
    """
    Main intelligence engine that orchestrates all editor features.
    
    Attributes:
        jedi_provider: JediCompletionProvider
        parso_parser: ParsoASTParser
        pyflakes_linter: PyflakesLinter
        cabe_analyzer: CabeComplexityAnalyzer
        rope_refactorer: RopeRefactorer
    """

    def __init__(self):
        """Initialize all sub-engines with lazy loading."""
        self._jedi_provider = None
        self._parso_parser = None
        self._pyflakes_linter = None
        self._cabe_analyzer = None
        self._rope_refactorer = None
        
        logger.info("Spike Intelligence Engine initialized (lazy load)")

    @property
    def jedi_provider(self):
        """Lazy load Jedi provider."""
        if self._jedi_provider is None:
            from .jedi_layer import JediCompletionProvider
            self._jedi_provider = JediCompletionProvider()
        return self._jedi_provider

    @property
    def parso_parser(self):
        """Lazy load Parso parser."""
        if self._parso_parser is None:
            from .parso_layer import ParsoASTParser
            self._parso_parser = ParsoASTParser()
        return self._parso_parser

    @property
    def pyflakes_linter(self):
        """Lazy load Pyflakes linter."""
        if self._pyflakes_linter is None:
            from .pyflakes_layer import PyflakesLinter
            self._pyflakes_linter = PyflakesLinter()
        return self._pyflakes_linter

    @property
    def cabe_analyzer(self):
        """Lazy load Cabe analyzer."""
        if self._cabe_analyzer is None:
            from .cabe_layer import CabeComplexityAnalyzer
            self._cabe_analyzer = CabeComplexityAnalyzer()
        return self._cabe_analyzer

    @property
    def rope_refactorer(self):
        """Lazy load Rope refactorer."""
        if self._rope_refactorer is None:
            from .rope_layer import RopeRefactorer
            self._rope_refactorer = RopeRefactorer()
        return self._rope_refactorer

    def autocomplete(self, code: str, position: Tuple[int, int]) -> List[Dict[str, Any]]:
        """Get code completions at given position."""
        return self.jedi_provider.get_completions(code, position)

    def parse_ast(self, code: str) -> Any:
        """Parse code into AST."""
        return self.parso_parser.parse(code)

    def lint(self, code: str) -> List[Dict[str, Any]]:
        """Run static analysis on code."""
        return self.pyflakes_linter.lint(code)

    def analyze_complexity(self, code: str) -> Dict[str, Any]:
        """Analyze code complexity metrics."""
        return self.cabe_analyzer.analyze(code)

    def refactor(self, code: str, operation: str, **kwargs) -> str:
        """Perform code refactoring."""
        return self.rope_refactorer.refactor(code, operation, **kwargs)

    def health_check(self) -> Dict[str, bool]:
        """Check if all sub-engines are operational."""
        return {
            "jedi": self._check_jedi(),
            "parso": self._check_parso(),
            "pyflakes": self._check_pyflakes(),
            "cabe": self._check_cabe(),
            "rope": self._check_rope()
        }

    def _check_jedi(self) -> bool:
        try:
            _ = self.jedi_provider
            return True
        except Exception as e:
            logger.warning(f"Jedi health check failed: {e}")
            return False

    def _check_parso(self) -> bool:
        try:
            _ = self.parso_parser
            return True
        except Exception as e:
            logger.warning(f"Parso health check failed: {e}")
            return False

    def _check_pyflakes(self) -> bool:
        try:
            _ = self.pyflakes_linter
            return True
        except Exception as e:
            logger.warning(f"Pyflakes health check failed: {e}")
            return False

    def _check_cabe(self) -> bool:
        try:
            _ = self.cabe_analyzer
            return True
        except Exception as e:
            logger.warning(f"Cabe health check failed: {e}")
            return False

    def _check_rope(self) -> bool:
        try:
            _ = self.rope_refactorer
            return True
        except Exception as e:
            logger.warning(f"Rope health check failed: {e}")
            return False