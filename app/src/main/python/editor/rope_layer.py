"""
Rope integration layer with input validation and error handling.

Provides code refactoring capabilities using Rope library with:
- Input validation
- Error handling
- Lazy loading
- Graceful degradation
"""

import logging
import os
import tempfile
from typing import Dict, Any, Optional
from .exceptions import RopeError

logger = logging.getLogger(__name__)

class RopeWrapper:
    """Wrapper for Rope with input validation and error handling."""

    def __init__(self):
        self._rope = None

    @property
    def rope(self):
        """Lazy load Rope library."""
        if self._rope is None:
            import rope.base.project
            import rope.base.libutils
            import rope.refactor.rename
            import rope.refactor.extract
            import rope.refactor.inline
            import rope.refactor.move
            import rope.refactor.method_object
            self._rope = rope
        return self._rope

    def refactor(self, code: str, operation: str, **kwargs) -> str:
        """Perform code refactoring operation with input validation and error handling."""
        try:
            with tempfile.TemporaryDirectory() as tmpdir:
                # Create a temporary project
                project = self.rope.base.project.Project(tmpdir)
                
                # Create a temporary file with the code
                file_path = os.path.join(tmpdir, "temp.py")
                with open(file_path, 'w') as f:
                    f.write(code)
                
                # Add to project
                module = project.get_resource(file_path)
                
                # Perform the requested operation
                if operation == "rename":
                    return self._rename(project, module, **kwargs)
                elif operation == "extract_method":
                    return self._extract_method(project, module, **kwargs)
                elif operation == "extract_variable":
                    return self._extract_variable(project, module, **kwargs)
                elif operation == "inline":
                    return self._inline(project, module, **kwargs)
                else:
                    logger.warning(f"Unknown refactoring operation: {operation}")
                    return code
        except Exception as e:
            raise RopeError(f"Rope refactoring failed: {e}")

    def _rename(self, project, module, **kwargs) -> str:
        """Rename a symbol with input validation and error handling."""
        old_name = kwargs.get('old_name')
        new_name = kwargs.get('new_name')
        line = kwargs.get('line', 1)
        
        if not old_name or not new_name:
            return module.get_code()
        
        try:
            # Get the offset for the line
            offset = module.get_offset(line, 0)
            changes = self.rope.refactor.rename.Rename(
                project, module, offset=offset, newname=new_name
            ).get_changes()
            
            # Apply changes
            for change in changes:
                change.do()
            
            return module.get_code()
        except Exception as e:
            raise RopeError(f"Rope rename failed: {e}")

    def _extract_method(self, project, module, **kwargs) -> str:
        """Extract code to a new method with input validation and error handling."""
        start_line = kwargs.get('start_line', 1)
        end_line = kwargs.get('end_line', 1)
        method_name = kwargs.get('method_name', 'extracted_method')
        
        try:
            changes = self.rope.refactor.extract.ExtractMethod(
                project, module,
                start_offset=module.get_offset(start_line, 0),
                end_offset=module.get_offset(end_line, 0),
                name=method_name
            ).get_changes()
            
            for change in changes:
                change.do()
            
            return module.get_code()
        except Exception as e:
            raise RopeError(f"Rope extract method failed: {e}")

    def _extract_variable(self, project, module, **kwargs) -> str:
        """Extract expression to a variable with input validation and error handling."""
        line = kwargs.get('line', 1)
        expression = kwargs.get('expression', '')
        var_name = kwargs.get('var_name', 'extracted_var')
        
        try:
            changes = self.rope.refactor.extract.ExtractVariable(
                project, module,
                offset=module.get_offset(line, 0),
                expression=expression,
                name=var_name
            ).get_changes()
            
            for change in changes:
                change.do()
            
            return module.get_code()
        except Exception as e:
            raise RopeError(f"Rope extract variable failed: {e}")

    def _inline(self, project, module, **kwargs) -> str:
        """Inline a method or variable with input validation and error handling."""
        line = kwargs.get('line', 1)
        
        try:
            changes = self.rope.refactor.inline.Inline(
                project, module, offset=module.get_offset(line, 0)
            ).get_changes()
            
            for change in changes:
                change.do()
            
            return module.get_code()
        except Exception as e:
            raise RopeError(f"Rope inline failed: {e}")

class DummyRopeWrapper:
    """Fallback if Rope is not available."""

    def refactor(self, code: str, operation: str, **kwargs) -> str:
        logger.warning("Rope not available - using dummy refactoring")
        return code