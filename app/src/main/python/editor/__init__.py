"""
Editor Intelligence Package for ZCODE.

Includes layers for:
- Jedi (Completion & Go-to definition)
- Parso (AST Parsing)
- Pyflakes (Linting & Static Analysis)
- Cabe (McCabe Complexity Analysis)
- Rope (Refactoring)
"""

class SpikeError(Exception):
    """Base class for all Spike Intelligence Engine errors."""
    pass

class JediError(SpikeError):
    """Exception raised by Jedi layer operations."""
    pass

class ParsoError(SpikeError):
    """Exception raised by Parso layer operations."""
    pass

class PyflakesError(SpikeError):
    """Exception raised by Pyflakes layer operations."""
    pass

class CabeError(SpikeError):
    """Exception raised by Cabe layer operations."""
    pass

class RopeError(SpikeError):
    """Exception raised by Rope layer operations."""
    pass

__all__ = [
    "SpikeError",
    "JediError",
    "ParsoError",
    "PyflakesError",
    "CabeError",
    "RopeError",
]
