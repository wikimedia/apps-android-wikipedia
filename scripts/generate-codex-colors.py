import requests

raw_color = requests.get("https://cdn.jsdelivr.net/npm/@wikimedia/codex-design-tokens@latest/dist/theme-wikimedia-ui.json")

def extract_colors(color_data):
    raw_colors = {}
    color_section = color_data.get('color', color_data)

    for name, color_info in color_section.items():
        if (isinstance(color_info, dict)
                and "attributes" in color_info
                and "type" in color_info["attributes"]
                and color_info["attributes"]["type"] == "theme"
                and 'value' in color_info):
            if ('-' not in name and
                    any(char.isdigit() for char in name) and
                    name[-1].isdigit()):
                raw_colors[name] = color_info['value']
    return raw_colors

def convert_hex_to_kotlin_color(hex_color):
    hex_color = hex_color.lstrip("#")
    return f"0xFF{hex_color}"

def generate_compose_raw_color_file(colors_dict, file_path):
    content = []
    content.append("package org.wikipedia.compose.theme\n\n")
    content.append("import androidx.compose.ui.graphics.Color\n\n")
    content.append("object ComposeColors { \n")

    sorted_colors = sorted(colors_dict.items())
    for color_name, hex_value in sorted_colors:
        var_name = color_name
        hex_value = convert_hex_to_kotlin_color(hex_value)
        content.append(f"  val {var_name} = Color({hex_value})\n")
    content.append("}")
    with open(file_path, 'w') as f:
        f.write("".join(content))

raw_colors = extract_colors(raw_color.json()['color'])
file_path = "../app/src/main/java/org/wikipedia/compose/theme/test.kt"
generate_compose_raw_color_file(raw_colors, file_path)

