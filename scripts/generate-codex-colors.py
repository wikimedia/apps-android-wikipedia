import requests
import re

# there are three type of color tokens from Codex
# 1. theme (raw colors like red100, white, accent, gray50 etc. file is under src/themes/wikimedia-ui.json)
# 2. base (color roles that references the theme colors like placeholder, progressive, disabled, destructive etc. file is under src/application.json)
# 3. component (color for specific ui components like table, footer etc. file is under src/components.json)
# 4. mode (dark mode colors)

# URL
codex_main_tokens_url = "https://cdn.jsdelivr.net/npm/@wikimedia/codex-design-tokens@latest/dist/theme-wikimedia-ui.json"
codex_dark_mode_tokens_url = "https://cdn.jsdelivr.net/npm/@wikimedia/codex-design-tokens@latest/dist/theme-wikimedia-ui-mode-dark.json"

# color_data: the codex json response
# token_type: one of the above token types (theme, base, component, mode)
def extract_colors(color_data, token_type):
    results = {}
    color_section = color_data.get('color', color_data)

    for name, color_info in color_section.items():
        if (isinstance(color_info, dict)
                and "attributes" in color_info
                and "type" in color_info["attributes"]
                and color_info["attributes"]["type"] == token_type
                and 'value' in color_info
                and color_info['value'].startswith("#")):
            hex_value = color_info['value']
            results[name] = hex_value
    return results

# takes in #f8f9fa and converts to 0xFFf8f9fa
def convert_hex_to_kotlin_color(hex_color):
    hex_color = hex_color.lstrip("#")
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
    content = ["package org.wikipedia.compose.theme\n\n",
               "import androidx.compose.ui.graphics.Color\n\n",
               "object ComposeColors { \n"]

    sorted_colors = sorted(colors_dict.items())
    for color_name, hex_value in sorted_colors:
        var_name = color_name
        hex_value = convert_hex_to_kotlin_color(hex_value)
        content.append(f"  val {var_name} = Color({hex_value})\n")
    content.append("}")
    with open(file_path, 'w') as f:
        f.write("".join(content))

# this generates the class with the codex tokens that will use the raw colors
# this will generate class similar to our current WikipediaColor
def generate_wikipedia_color(main_color_data, file_path):
    base_colors = extract_colors(main_color_data, "base")
    content = ["package org.wikipedia.compose.theme\n\n",
               "import androidx.compose.runtime.Immutable\n"
               "import androidx.compose.ui.graphics.Color\n\n",
               "@Immutable\ndata class WikipediaColor(\n"]
    pascal_names = [to_camel_case(name) for name in base_colors.keys()]
    for name in pascal_names:
        content.append(f"\tval {name}: Color,\n")
    content.append(")")
    with open(file_path, 'w') as f:
        f.write("".join(content))
    return pascal_names

# raw colors
def generate_raw_colors(main_color_data, file_path):
    raw_colors = extract_colors(main_color_data, "theme")
    generate_compose_raw_color_file(raw_colors, file_path)

# light mode tokens
def generate_light_mode_tokens():
    light_mode_response = requests.get(codex_dark_mode_tokens_url)
    light_mode_color_data = light_mode_response.json()['color']
    print("TODO after the m3 alignment work")

# dark mode tokens
def generate_dark_mode_tokens():
    dark_mode_response = requests.get(codex_dark_mode_tokens_url)
    dark_mode_color_data = dark_mode_response.json()['color']
    print("TODO after the m3 alignment work")

# execution
# 1. generate raw colors
# 2. generate WikipediaColor data class with codex tokens
# 3. generate light mode colors and append to WikipediaColor file
# 4. generate dark mode colors and append to WikipediaColor file

wiki_color_file_path = "../app/src/main/java/org/wikipedia/compose/theme/WikipediaColor.kt"
raw_color_file_path =  "../app/src/main/java/org/wikipedia/compose/theme/ComposeColors.kt"

response = requests.get(codex_main_tokens_url)
main_color_data = response.json()['color']
generate_raw_colors(main_color_data, file_path = raw_color_file_path)
generate_wikipedia_color(main_color_data, file_path= wiki_color_file_path)

