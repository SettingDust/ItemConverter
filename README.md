# 📦 Item Converter

This mod introduces powerful features to enhance inventory management and crafting efficiency.  
It allows you to convert items in your inventory, perform ratio-based item conversions.  

## 🔄 Data-Driven Item Conversion

- Press a key while selecting an item in your inventory to open a conversion GUI.
- The GUI shows item slots based on rules defined in data packs, allowing you to perform ratio-based item conversions.
- All conversion rules are fully data-driven and customizable, providing flexibility for pack developers and players alike.

## 🛠️ Auto Conversion with Middle-Click

- Target a block and middle-click to automatically search your inventory for the usable rules and convert the block directly.

## ⚙️ Data Pack Support

The mod relies on data packs for defining conversion rules, supporting two types of JSON files:

1. **`item_converter/rules`**: Defines the specific conversion rules, specifying input items, output items, and conversion ratios.
2. **`item_converter/rule_generator`**: Generates conversion rules based on existing recipes, simplifying the setup process.

### 📋 Command

Use the following command to auto-generate conversion rules from generator:
```
/item_converter generate [generator]
```