"""
Cabe (McCabe) Complexity Analysis Layer

Provides code complexity analysis using McCabe metrics.
"""

import logging
import ast
from typing import Dict, Any

logger = logging.getLogger(__name__)

class CabeWrapper:
    """Wrapper for code complexity analysis using McCabe metrics."""

    def __init__(self):
        self.available = True  # Cabe is pure Python, no external deps

    def analyze(self, code: str) -> Dict[str, Any]:
        """Analyze code complexity using McCabe metrics."""
        try:
            tree = ast.parse(code)
            
            # Calculate cyclomatic complexity
            complexity = self._calculate_complexity(tree)
            
            # Count functions/classes
            func_count = sum(1 for node in ast.walk(tree) if isinstance(node, ast.FunctionDef))
            class_count = sum(1 for node in ast.walk(tree) if isinstance(node, ast.ClassDef))
            
            return {
                "cyclomatic_complexity": complexity,
                "function_count": func_count,
                "class_count": class_count,
                "maintainability_index": self._calculate_maintainability(complexity, func_count),
                "status": "success"
            }
        except SyntaxError as e:
            logger.error(f"Syntax error in complexity analysis: {e}")
            return {
                "error": f"Syntax error: {e.msg}",
                "line": e.lineno,
                "column": e.offset,
                "status": "error"
            }
        except Exception as e:
            logger.error(f"Complexity analysis error: {e}")
            return {
                "error": str(e),
                "status": "error"
            }

    def _calculate_complexity(self, node: ast.AST, complexity: int = 1) -> int:
        """Recursively calculate cyclomatic complexity."""
        if isinstance(node, (ast.If, ast.For, ast.While, ast.And, ast.Or, ast.ExceptHandler)):
            complexity += 1
        
        for child in ast.iter_child_nodes(node):
            complexity = self._calculate_complexity(child, complexity)
        
        return complexity

    def _calculate_maintainability(self, complexity: int, func_count: int) -> float:
        """Calculate a simple maintainability index."""
        if func_count == 0:
            return 100.0  # Perfect score for empty code
            
        # Simple formula: lower complexity and fewer functions = better
        return max(0, 100 - (complexity * 2) - (func_count * 0.5))

class DummyCabeWrapper:
    """Fallback if Cabe is not available."""

    def analyze(self, code: str) -> Dict[str, Any]:
        logger.warning("Cabe not available - using dummy analysis")
        return {
            "cyclomatic_complexity": 0,
            "function_count": 0,
            "class_count": 0,
            "maintainability_index": 0,
            "status": "unavailable"
        }