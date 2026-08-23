"""
Rope Refactoring Layer

Provides code refactoring capabilities using Rope library.
"""

import logging
import os
import tempfile
from typing import Dict, Any, Optional

logger = logging.getLogger(__name__)

class RopeRefactorer:
    """Wrapper around Rope library for code refactoring."""

    def __init__(self):
        try:
            import rope.base.project
            import rope.base.libutils
            import rope.refactor.rename
            import rope.refactor.extract
            import rope.refactor.inline
            import rope.refactor.move
            import rope.refactor.method_object
            
            self.rope = rope
            self.available = True
            logger.info("Rope initialized successfully")
        except ImportError:
            self.available = False
            logger.warning("Rope not available - refactoring will be limited")

    def refactor(self, code: str, operation: str, **kwargs) -> str:
        """Perform code refactoring operation."""
        if not self.available:
            return code

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
            logger.error(f"Rope refactoring error: {e}")
            return code

    def _rename(self, project, module, **kwargs) -> str:
        """Rename a symbol."""
        old_name = kwargs.get('old_name')
        new_name = kwargs.get('new_name')
        line = kwargs.get('line', 1)
        
        if not old_name or not new_name:
            return module.get_code()
        
        # Get the offset for the line
        changes = self.rope.refactor.rename.Rename(
            project, module, offset=module.get_offset(line, 0), newname=new_name
        ).get_changes()
        
        # Apply changes
        for change in changes:
            change.do()
        
        return module.get_code()

    def _extract_method(self, project, module, **kwargs) -> str:
        """Extract code to a new method."""
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
            logger.error(f"Extract method failed: {e}")
            return module.get_code()

    def _extract_variable(self, project, module, **kwargs) -> str:
        """Extract expression to a variable."""
        line = kwargs.get('line', 1)
        expression = kwargs.get('expression', '')
        var_name = kwargs.get('var_name', 'extracted_var')
        
        try:
            # This is a simplified version - actual implementation would need
            # to find the exact expression in the code
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
            logger.error(f"Extract variable failed: {e}")
            return module.get_code()

    def _inline(self, project, module, **kwargs) -> str:
        """Inline a method or variable."""
        line = kwargs.get('line', 1)
        
        try:
            changes = self.rope.refactor.inline.Inline(
                project, module, offset=module.get_offset(line, 0)
            ).get_changes()
            
            for change in changes:
                change.do()
            
            return module.get_code()
        except Exception as e:
            logger.error(f"Inline failed: {e}")
            return module.get_code()