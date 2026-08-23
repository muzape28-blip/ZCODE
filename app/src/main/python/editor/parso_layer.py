"""
Parso Integration Layer

Provides AST parsing and syntax analysis using Parso library.
"""

import logging
from typing import Any

logger = logging.getLogger(__name__)

class ParsoASTParser:
    """Wrapper around Parso library for AST parsing."""

    def __init__(self):
        try:
            import parso
            self.parso = parso
            self.available = True
            logger.info("Parso initialized successfully")
        except ImportError:
            self.available = False
            logger.warning("Parso not available - AST parsing will be limited")

    def parse(self, code: str) -> Any:
        """Parse code into AST using Parso."""
        if not self.available:
            return None

        try:
            grammar = self.parso.load_grammar()
            module = grammar.parse(code)
            return module
        except Exception as e:
            logger.error(f"Parso parsing error: {e}")
            return None

    def get_errors(self, code: str) -> list:
        """Get syntax errors from code."""
        if not self.available:
            return []

        try:
            grammar = self.parso.load_grammar()
            module = grammar.parse(code)
            return [{
                "message": error.message,
                "line": error.start_pos[0],
                "column": error.start_pos[1]
            } for error in grammar.iter_errors(module)]
        except Exception as e:
            logger.error(f"Parso error detection failed: {e}")
            return []