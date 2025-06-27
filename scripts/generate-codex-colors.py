#!/usr/bin/env python
# coding=utf-8
import requests
import re

# there are three type of color tokens from Codex
# 1. theme (raw colors like red100, white, accent, gray50 etc. file is under src/themes/wikimedia-ui.json)
# 2. base (color roles that references the theme colors like placeholder, progressive, disabled, destructive etc. file is under src/application.json)
# 3. component (color for specific ui components like table, footer etc. file is under src/components.json)
# 4. mode (dark mode colors)

CODEX_VERSION = 2.1

# URL
CODEX_MAIN_TOKENS_URL = f"https://cdn.jsdelivr.net/npm/@wikimedia/codex-design-tokens@{CODEX_VERSION}/dist/theme-wikimedia-ui.json"
CODEX_DARK_MODE_TOKENS_URL = "https://cdn.jsdelivr.net/npm/@wikimedia/codex-design-tokens@latest/dist/theme-wikimedia-ui-mode-dark.json"

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
def extract_colors(color_data, token_type, dark_mode = False):
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
                    # Extract rawColor name from the value
                    match = re.search(r'color\.(\w+)', original_value)
                    if match:
                        pascal_names = to_camel_case(name)
                        results[pascal_names] = match.group(1)
            else:
                if ("value" in color_info
                and color_info["value"].startswith("#")):
                    hex_value = color_info["value"]
                    results[name] = convert_hex_to_kotlin_color(hex_value)
    return results

# takes in #f8f9fa and converts to 0xFFf8f9fa
# handles 3 digit css color as well
def convert_hex_to_kotlin_color(hex_color):
    hex_color = hex_color.lstrip("#")
    # handle 3-digit CSS color
    if len(hex_color) == 3:
        hex_color = ''.join([char * 2 for char in hex_color])
    return f"0xFF{hex_color}"

def to_camel_case(text):
    # Split on both single and double dashes
    parts = re.split(r'-+', text)
    if not parts:
        return text
    # First part will be lowercase, rest are capitalized
    return parts[0].lower() + ''.join(word.capitalize() for word in parts[1:] if word)


# colors_dict: the processed codex colors with { name: value } format
# file_path: path where you want to save the file
def generate_compose_raw_color_file(colors_dict, file_path):
    if not colors_dict:
        print("Warning: No raw colors to generate class")
        return False

    content = ["package org.wikipedia.compose\n\n",
               "import androidx.compose.ui.graphics.Color\n\n",
               f"// CODEX VERSION {CODEX_VERSION}\n",
               "object ComposeColors {\n"]
    sorted_colors = sorted(colors_dict.items())
    for color_name, hex_value in sorted_colors:
        content.append(f"  val {color_name} = Color({hex_value})\n")
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

    content = ["package org.wikipedia.compose.theme\n\n",
               "import androidx.compose.runtime.Immutable\n"
               "import androidx.compose.ui.graphics.Color\n",
               "import org.wikipedia.compose.ComposeColors\n\n"
               f"// CODEX VERSION {CODEX_VERSION}\n",
               "@Immutable\ndata class WikipediaColor(\n"]
    for name in base_colors.keys():
        content.append(f"\tval {name}: Color,\n")
    content.append(")\n")

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

    content = [f"\nval {mode_name}Colors = WikipediaColor(\n"]
    for name, original_value_name in sorted(colors.items()):
        content.append(f"\t{name} = ComposeColors.{original_value_name},\n")
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
RAW_COLOR_FILE_PATH =  "../app/src/main/java/org/wikipedia/compose/ComposeColors.kt"

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

