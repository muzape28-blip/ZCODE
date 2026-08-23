"""
ZCODE Plugin System

Core plugin management for ZCODE editor and runtime.
"""

import logging
import importlib
from typing import Dict, Type, Any

logger = logging.getLogger(__name__)

class PluginManager:
    """Manages all ZCODE plugins."""
    
    def __init__(self):
        self.plugins: Dict[str, Any] = {}
        self._initialize_core_plugins()

    def _initialize_core_plugins(self):
        """Initialize core plugins that ship with ZCODE."""
        # Register core plugins
        self.register_plugin('package_manager', 'zcode_pip.PackageManager')
        self.register_plugin('runtime', 'zcode_runner.ZCodeRuntime')
        
        # Add Spike Intelligence Engine as a core plugin
        try:
            self.register_plugin('spike_intelligence', 'editor.intelligence_engine.SpikeIntelligenceEngine')
            logger.info("Spike Intelligence Engine registered as core plugin")
        except Exception as e:
            logger.error(f"Failed to register Spike Intelligence Engine: {e}")

    def register_plugin(self, name: str, import_path: str):
        """Register a new plugin."""
        try:
            module_name, class_name = import_path.rsplit('.', 1)
            module = importlib.import_module(module_name)
            plugin_class = getattr(module, class_name)
            
            # Initialize the plugin
            self.plugins[name] = plugin_class()
            logger.info(f"Plugin {name} registered successfully")
            
            return True
        except Exception as e:
            logger.error(f"Failed to register plugin {name}: {e}")
            return False

    def get_plugin(self, name: str) -> Any:
        """Get a registered plugin."""
        return self.plugins.get(name)

    def list_plugins(self) -> Dict[str, str]:
        """List all registered plugins."""
        return {name: type(plugin).__name__ for name, plugin in self.plugins.items()}

# Global plugin manager instance
plugin_manager = PluginManager()