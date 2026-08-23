"""
Parso Integration Layer with Lazy Loading and Caching

Provides AST parsing and syntax analysis using Parso library.
"""

import logging
from functools import lru_cache
from typing import Any, Optional, List, Dict

logger = logging.getLogger(__name__)

class ParsoWrapper:
    """Wrapper around Parso library for AST parsing with caching."""

    def __init__(self, parso):
        self._parso = parso

    @lru_cache(maxsize=100)
    def parse(self, code: str) -> Optional[Any]:
        """Parse code into AST with caching."""
        try:
            grammar = self._parso.load_grammar()
            return grammar.parse(code)
        except Exception as e:
            logger.error(f"Parso parsing error: {e}")
            return None

    def get_errors(self, code: str) -> List[Dict[str, Any]]:
        """Get syntax errors from code."""
        try:
            tree = self.parse(code)
            if tree is None:
                return []
            return [{
                "message": error.message,
                "line": error.start_pos[0],
                "column": error.start_pos[1]
            } for error in self._parso.iter_errors(tree)]
        except Exception as e:
            logger.error(f"Parso error detection failed: {e}")
            return []

class DummyParsoWrapper:
    """Fallback if Parso is not available."""

    def parse(self, code: str) -> Optional[Any]:
        logger.warning("Parso not available - using dummy parser")
        return None

    def get_errors(self, code: str) -> List[Dict[str, Any]]:
        logger.warning("Parso not available - using dummy errors")
        return []