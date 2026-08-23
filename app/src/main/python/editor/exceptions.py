"""
Exception hierarchy for Spike Intelligence Engine.

Base class for all Spike Intelligence errors, with specific exceptions for each component.
"""

class SpikeError(Exception):
    """Base class for Spike Intelligence errors."""
    pass

class JediError(SpikeError):
    """Jedi-specific errors."""
    pass

class ParsoError(SpikeError):
    """Parso-specific errors."""
    pass

class PyflakesError(SpikeError):
    """Pyflakes-specific errors."""
    pass

class CabeError(SpikeError):
    """Cabe-specific errors."""
    pass

class RopeError(SpikeError):
    """Rope-specific errors."""
    pass