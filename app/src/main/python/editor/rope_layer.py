"""
Rope Refactoring Layer with Lazy Loading and Error Handling

Provides code refactoring capabilities using Rope library.
"""

import logging
import os
import tempfile
from typing import Dict, Any, Optional

logger = logging.getLogger(__name__)

class RopeWrapper:
    """Wrapper around Rope library for code refactoring."""

    def __init__(self, rope):
        self._rope = rope

    def refactor(self, code: str, operation: str, **kwargs) -> str:
        """Perform code refactoring operation."""
        try:
            with tempfile.TemporaryDirectory() as tmpdir:
                # Create a temporary project
                project = self._rope.base.project.Project(tmpdir)
                
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
            logger.error(f"Rope refactoring error: {e}")
            return code

    def _rename(self, project, module, **kwargs) -> str:
        """Rename a symbol."""
        old_name = kwargs.get('old_name')
        new_name = kwargs.get('new_name')
        line = kwargs.get('line', 1)
        
        if not old_name or not new_name:
            return module.get_code()
        
        try:
            # Get the offset for the line
            offset = module.get_offset(line, 0)
            changes = self._rope.refactor.rename.Rename(
                project, module, offset=offset, newname=new_name
            ).get_changes()
            
            # Apply changes
            for change in changes:
                change.do()
            
            return module.get_code()
        except Exception as e:
            logger.error(f"Rope rename failed: {e}")
            return module.get_code()

    def _extract_method(self, project, module, **kwargs) -> str:
        """Extract code to a new method."""
        start_line = kwargs.get('start_line', 1)
        end_line = kwargs.get('end_line', 1)
        method_name = kwargs.get('method_name', 'extracted_method')
        
        try:
            changes = self._rope.refactor.extract.ExtractMethod(
                project, module,
                start_offset=module.get_offset(start_line, 0),
                end_offset=module.get_offset(end_line, 0),
                name=method_name
            ).get_changes()
            
            for change in changes:
                change.do()
            
            return module.get_code()
        except Exception as e:
            logger.error(f"Rope extract method failed: {e}")
            return module.get_code()

    def _extract_variable(self, project, module, **kwargs) -> str:
        """Extract expression to a variable."""
        line = kwargs.get('line', 1)
        expression = kwargs.get('expression', '')
        var_name = kwargs.get('var_name', 'extracted_var')
        
        try:
            changes = self._rope.refactor.extract.ExtractVariable(
                project, module,
                offset=module.get_offset(line, 0),
                expression=expression,
                name=var_name
            ).get_changes()
            
            for change in changes:
                change.do()
            
            return module.get_code()
        except Exception as e:
            logger.error(f"Rope extract variable failed: {e}")
            return module.get_code()

    def _inline(self, project, module, **kwargs) -> str:
        """Inline a method or variable."""
        line = kwargs.get('line', 1)
        
        try:
            changes = self._rope.refactor.inline.Inline(
                project, module, offset=module.get_offset(line, 0)
            ).get_changes()
            
            for change in changes:
                change.do()
            
            return module.get_code()
        except Exception as e:
            logger.error(f"Rope inline failed: {e}")
            return module.get_code()

class DummyRopeWrapper:
    """Fallback if Rope is not available."""

    def refactor(self, code: str, operation: str, **kwargs) -> str:
        logger.warning("Rope not available - using dummy refactoring")
        return code