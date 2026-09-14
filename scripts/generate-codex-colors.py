#!/usr/bin/env python
# coding=utf-8
import os
import re

import requests

# there are three type of color tokens from Codex
# 1. theme (raw colors like red100, white, accent, gray50 etc. file is under src/themes/wikimedia-ui.json)
# 2. base (color roles that references the theme colors like placeholder, progressive, disabled, destructive etc. file is under src/application.json)
# 3. component (color for specific ui components like table, footer etc. file is under src/components.json)
# 4. mode (dark mode colors)

CODEX_VERSION = 2.7

# URL
CODEX_MAIN_TOKENS_URL = f"https://cdn.jsdelivr.net/npm/@wikimedia/codex-design-tokens@{CODEX_VERSION}/dist/theme-wikimedia-ui.json"
CODEX_DARK_MODE_TOKENS_URL = "https://cdn.jsdelivr.net/npm/@wikimedia/codex-design-tokens@latest/dist/theme-wikimedia-ui-mode-dark.json"

LEGACY_WIKIPEDIA_FIELDS = [
    "primaryColor",
    "paperColor",
    "backgroundColor",
    "inactiveColor",
    "placeholderColor",
    "secondaryColor",
    "borderColor",
    "progressiveColor",
    "successColor",
    "destructiveColor",
    "warningColor",
    "highlightColor",
    "focusColor",
    "additionColor",
    "overlayColor",
]

LEGACY_LIGHT_COLORS = {
    "primaryColor": "Gray700",
    "paperColor": "White",
    "backgroundColor": "Gray100",
    "inactiveColor": "Gray400",
    "placeholderColor": "Gray500",
    "secondaryColor": "Gray600",
    "borderColor": "Gray200",
    "progressiveColor": "Blue600",
    "successColor": "Green700",
    "destructiveColor": "Red700",
    "warningColor": "Yellow700",
    "highlightColor": "Yellow500",
    "focusColor": "Orange500",
    "additionColor": "Blue300_15",
    "overlayColor": "Black_30",
}

LEGACY_DARK_COLORS = {
    "primaryColor": "Gray200",
    "paperColor": "Gray700",
    "backgroundColor": "Gray675",
    "inactiveColor": "Gray500",
    "placeholderColor": "Gray400",
    "secondaryColor": "Gray300",
    "borderColor": "Gray650",
    "progressiveColor": "Blue300",
    "successColor": "Green600",
    "destructiveColor": "Red500",
    "warningColor": "Orange500",
    "highlightColor": "Yellow500_40",
    "focusColor": "Orange500_50",
    "additionColor": "Blue600_30",
    "overlayColor": "Black_70",
}

LEGACY_BLACK_COLORS = {
    "primaryColor": "Gray200",
    "paperColor": "Black",
    "backgroundColor": "Gray700",
    "inactiveColor": "Gray500",
    "placeholderColor": "Gray500",
    "secondaryColor": "Gray300",
    "borderColor": "Gray675",
    "progressiveColor": "Blue300",
    "successColor": "Green600",
    "destructiveColor": "Red500",
    "warningColor": "Orange500",
    "highlightColor": "Yellow500_40",
    "focusColor": "Orange500_50",
    "additionColor": "Blue600_30",
    "overlayColor": "Black_70",
}

LEGACY_SEPIA_COLORS = {
    "primaryColor": "Gray700",
    "paperColor": "Beige100",
    "backgroundColor": "Beige300",
    "inactiveColor": "Taupe200",
    "placeholderColor": "Taupe600",
    "secondaryColor": "Gray600",
    "borderColor": "Beige400",
    "progressiveColor": "Blue600",
    "successColor": "Green700",
    "destructiveColor": "Red700",
    "warningColor": "Yellow700",
    "highlightColor": "Yellow500",
    "focusColor": "Orange500",
    "additionColor": "Blue300_15",
    "overlayColor": "Black_30",
}

LEGACY_MODE_COLORS = {
    "Light": LEGACY_LIGHT_COLORS,
    "Dark": LEGACY_DARK_COLORS,
    "Black": LEGACY_BLACK_COLORS,
    "Sepia": LEGACY_SEPIA_COLORS,
}


def normalize_identifier(value):
    return re.sub(r"[^a-zA-Z0-9]", "", value).lower()


def to_pascal_case(text):
    parts = re.split(r"[-_\s]+", text)
    normalized = []
    for part in parts:
        if not part:
            continue
        normalized.append(part[:1].upper() + part[1:])
    return "".join(normalized)


# Fetch and return color data from URL with error handling
def fetch_color_data(url, description):
    print(f"fetching {description}")
    print(f"Making request to: {url}")
    try:
        response = requests.get(url)
        if response.status_code == 200:
            print("Request successful!")
            return response.json()["color"]
        else:
            print(f"Request failed with status code: {response.status_code}")
            return None
    except Exception as e:
        print(f"Error fetching {description}: {e}")
        return None


# color_data: the codex json response
# token_type: one of the above token types (theme, base, component, mode)
# dark mode tokens consist of two types base and mode, 3rd parameter dark_mode handles this edge case
def extract_colors(color_data, token_type, dark_mode=False):
    results = {}
    color_section = color_data.get('color', color_data)

    for name, color_info in color_section.items():
        if (isinstance(color_info, dict)
               and "attributes" in color_info
               and "type" in color_info["attributes"]
               and (color_info["attributes"]["type"] in ["base", "mode"] if dark_mode
               else color_info["attributes"]["type"] == token_type)):
            if token_type != "theme":
               if ("original" in color_info
                   and "value" in color_info["original"]):
                   original_value = color_info["original"]["value"]
                   match = re.search(r'color\.(\w+)', original_value)
                   if match:
                       results[name] = match.group(1)
            else:
               if ("value" in color_info
               and color_info["value"].startswith("#")):
                   hex_value = color_info["value"]
                   results[name] = convert_hex_to_kotlin_color(hex_value)
    return results


# takes in #f8f9fa and converts to 0xFFf8f9fa
# handles 3 digit css color as well
def convert_hex_to_kotlin_color(hex_color):
    hex_color = hex_color.lstrip("#").upper()
    # handle 3-digit CSS color
    if len(hex_color) == 3:
        hex_color = ''.join([char * 2 for char in hex_color])
    return f"0xFF{hex_color}"


def read_existing_color_names(file_path):
    if not os.path.exists(file_path):
        return [], {}, {}

    with open(file_path, "r", encoding="utf-8") as file:
        content = file.read()

    names = []
    by_key = {}
    values = {}
    for match in re.finditer(r"val\s+([A-Za-z0-9_]+)\s*=\s*Color\((0x[0-9A-Fa-f]+)\)", content):
        name = match.group(1)
        values[name] = match.group(2)
        if name not in names:
            names.append(name)
        by_key.setdefault(normalize_identifier(name), name)
    return names, by_key, values


def resolve_color_name(color_name, existing_names):
    normalized = normalize_identifier(color_name)
    if normalized in existing_names:
        return existing_names[normalized]
    return to_pascal_case(color_name) or color_name


# colors_dict: the processed codex colors with { name: value } format
# file_path: path where you want to save the file
def generate_compose_raw_color_file(colors_dict, file_path):
    if not colors_dict:
        print("Warning: No raw colors to generate class")
        return False

    existing_names, existing_by_key, existing_values = read_existing_color_names(file_path)
    ordered_names = list(existing_names)
    output_values = {name: existing_values.get(name, "0xFFFFFFFF") for name in ordered_names}
    used_names = set(existing_names)

    for color_name, hex_value in sorted(colors_dict.items()):
        resolved_name = resolve_color_name(color_name, existing_by_key)
        output_values[resolved_name] = hex_value
        if resolved_name not in used_names:
            ordered_names.append(resolved_name)
            used_names.add(resolved_name)

    content = ["package org.wikipedia.compose\n\n",
               "import androidx.compose.ui.graphics.Color\n\n",
               f"// CODEX VERSION {CODEX_VERSION}\n",
               "object ComposeColors {\n"]
    for color_name in ordered_names:
        content.append(f"    val {color_name} = Color({output_values[color_name]})\n")
    content.append("}\n")

    try:
        with open(file_path, 'w') as f:
            f.write("".join(content))
        print(f"Generated ComposeColors class: {file_path}")
        return True
    except Exception as e:
        print(f"Error writing ComposeColors class: {e}")
        return False


def generate_wikipedia_color_class(base_colors, file_path):
    if not base_colors:
        print("Warning: No base colors to generate class")
        return False

    property_names = list(LEGACY_WIKIPEDIA_FIELDS)

    content = [
        "package org.wikipedia.compose.theme\n\n",
        "import androidx.compose.runtime.Composable\n",
        "import androidx.compose.runtime.Immutable\n",
        "import androidx.compose.runtime.staticCompositionLocalOf\n",
        "import androidx.compose.ui.graphics.Color\n",
        "import org.wikipedia.compose.ComposeColors\n\n",
        f"// CODEX VERSION {CODEX_VERSION}\n",
        "@Immutable\n",
        "data class WikipediaColor(\n",
        "    val isDarkTheme: Boolean = false,\n",
    ]

    for name in property_names:
        if name == "isDarkTheme":
            continue
        content.append(f"    val {name}: Color,\n")
    content.append(")\n\n")

    content.append("val LocalWikipediaColor = staticCompositionLocalOf {\n")
    content.append("    WikipediaColor(\n")
    for name in property_names:
        if name == "isDarkTheme":
            continue
        content.append(f"        {name} = Color.Unspecified,\n")
    content.append("    )\n")
    content.append("}\n\n")
    content.extend([
        "@Composable\n",
        "fun WikipediaColor.shimmerColors(): List<Color> {\n",
        "    return if (isDarkTheme) {\n",
        "        listOf(\n",
        "            borderColor.copy(alpha = 0.3f),\n",
        "            inactiveColor.copy(alpha = 0.5f),\n",
        "            borderColor.copy(alpha = 0.3f)\n",
        "        )\n",
        "    } else {\n",
        "        listOf(\n",
        "            borderColor.copy(alpha = 0.6f),\n",
        "            backgroundColor.copy(alpha = 0.8f),\n",
        "            borderColor.copy(alpha = 0.6f)\n",
        "        )\n",
        "    }\n",
        "}\n",
    ])

    try:
        with open(file_path, 'w') as f:
            f.write("".join(content))
        print(f"Generated WikipediaColor class: {file_path}")
        return True
    except Exception as e:
        print(f"Error writing WikipediaColor class: {e}")
        return False


# Append light or dark color tokens to the file
def append_color_tokens(colors, file_path, mode_name):
    if not colors:
        print(f"Warning: No {mode_name} colors to append")
        return False

    mode_colors = LEGACY_MODE_COLORS.get(mode_name, {})
    _, existing_by_key, _ = read_existing_color_names(RAW_COLOR_FILE_PATH)

    content = [f"\nval {mode_name}Colors = WikipediaColor(\n"]
    if mode_name in {"Dark", "Black"}:
        content.append("    isDarkTheme = true,\n")
    for name in LEGACY_WIKIPEDIA_FIELDS:
        if name == "isDarkTheme":
            continue
        if mode_name in LEGACY_MODE_COLORS and name in mode_colors:
            value_name = mode_colors[name]
        else:
            normalized_candidates = {
               normalize_identifier(name.removesuffix("Color")),
               normalize_identifier(name),
            }
            value_name = None
            for token_name, _ in sorted(colors.items()):
               if normalize_identifier(token_name) in normalized_candidates:
                   value_name = resolve_color_name(token_name, existing_by_key)
                   break
            if value_name is None:
               continue
        content.append(f"    {name} = ComposeColors.{value_name},\n")
    content.append(")\n")

    try:
        with open(file_path, 'a') as f:
            f.write("".join(content))
        print(f"Appended {mode_name} colors to file")
        return True
    except Exception as e:
        print(f"Error appending {mode_name} colors: {e}")
        return False


WIKI_COLOR_FILE_PATH = "../app/src/main/java/org/wikipedia/compose/theme/WikipediaColor.kt"
RAW_COLOR_FILE_PATH = "../app/src/main/java/org/wikipedia/compose/ComposeColors.kt"

if __name__ == '__main__':
    print("=== Starting Codex Color Generation ===")
    # Step 1: Fetch all required data
    print("\n1. Fetching color data...")
    main_color_data = fetch_color_data(CODEX_MAIN_TOKENS_URL, "main codex colors")
    dark_color_data = fetch_color_data(CODEX_DARK_MODE_TOKENS_URL, "dark mode codex colors")

    # Step 2: Extract all colors
    print("\n2. Extracting colors...")
    raw_colors = extract_colors(main_color_data, "theme")
    light_colors = extract_colors(main_color_data, "base")
    dark_colors = extract_colors(dark_color_data, "mode", True)

    if raw_colors and light_colors and dark_colors:
        print("Color extraction completed")
    else:
        print("Color extraction failed")

    # Step 4: Generate all files
    print("\n4. Generating files...")
    generate_compose_raw_color_file(raw_colors, RAW_COLOR_FILE_PATH)
    generate_wikipedia_color_class(light_colors, WIKI_COLOR_FILE_PATH)
    append_color_tokens(light_colors, WIKI_COLOR_FILE_PATH, "Light")
    append_color_tokens(dark_colors, WIKI_COLOR_FILE_PATH, "Dark")
    append_color_tokens(dark_colors, WIKI_COLOR_FILE_PATH, "Black")
    append_color_tokens(light_colors, WIKI_COLOR_FILE_PATH, "Sepia")
