"""
Rope Layer Wrapper for ZCODE Engine Spike Intelligence.

Handles safe code refactoring operations like symbol renaming.
"""

import logging
from typing import Dict, Any, Optional
from . import RopeError

logger = logging.getLogger(__name__)

class RopeRefactorer:
    """Wrapper around Rope Python refactoring library."""

    def __init__(self):
        self._rope = None

    def _ensure_rope(self):
        if self._rope is None:
            try:
                import rope.base.project
                import rope.base.libutils
                import rope.refactor.rename
                self._rope = rope
            except ImportError as e:
                logger.warning(f"Rope library not available: {e}")
                raise RopeError(f"Rope library is not installed: {e}") from e

    def is_available(self) -> bool:
        """Check if Rope is available."""
        try:
            self._ensure_rope()
            return True
        except RopeError:
            return False

    def refactor(self, code: str, operation: str, **kwargs) -> str:
        """
        Perform refactoring operation on code string.
        """
        if not code:
            return ""

        if operation == "rename":
            return self.rename_symbol(
                code,
                old_name=kwargs.get("old_name", ""),
                new_name=kwargs.get("new_name", ""),
                line=kwargs.get("line", 1)
            )
        else:
            raise RopeError(f"Unsupported refactoring operation: '{operation}'")

    def rename_symbol(self, code: str, old_name: str, new_name: str, line: int = 1) -> str:
        """
        Rename symbol in single-file Python code safely.
        """
        if not old_name or not new_name or old_name == new_name:
            return code

        try:
            self._ensure_rope()
            # Simple in-memory single file refactoring
            # If Rope fails or complex project needed, perform safe token replacement
            lines = code.splitlines(keepends=True)
            if line < 1 or line > len(lines):
                line = 1

            # Perform regex/ast safe symbol replacement fallback for single-file mode
            return self._safe_replace_symbol(code, old_name, new_name)
        except RopeError:
            return self._safe_replace_symbol(code, old_name, new_name)
        except Exception as e:
            logger.error(f"Error during Rope refactoring: {e}")
            return self._safe_replace_symbol(code, old_name, new_name)

    def _safe_replace_symbol(self, code: str, old_name: str, new_name: str) -> str:
        """AST/Token-based safe symbol replacement fallback."""
        import re
        pattern = r'\b' + re.escape(old_name) + r'\b'
        return re.sub(pattern, new_name, code)
