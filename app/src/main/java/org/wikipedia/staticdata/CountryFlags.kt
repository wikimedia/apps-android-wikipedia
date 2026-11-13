package org.wikipedia.staticdata

/**
 * ISO 3166-1 alpha-2 country code -> 500px PNG rendering of the country's flag on Wikimedia Commons.
 * Generated 2026-09-30.
 */
object CountryFlags {
    private val urls: Map<String, String> = mapOf(
        "AC" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/65/Flag_of_Ascension_Island.svg/500px-Flag_of_Ascension_Island.svg.png", // Ascension
        "AD" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/19/Flag_of_Andorra.svg/500px-Flag_of_Andorra.svg.png", // Andorra
        "AE" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/cb/Flag_of_the_United_Arab_Emirates.svg/500px-Flag_of_the_United_Arab_Emirates.svg.png", // United Arab Emirates
        "AF" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/5c/Flag_of_the_Taliban.svg/500px-Flag_of_the_Taliban.svg.png", // Afghanistan
        "AG" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/89/Flag_of_Antigua_and_Barbuda.svg/500px-Flag_of_Antigua_and_Barbuda.svg.png", // Antigua and Barbuda
        "AI" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b4/Flag_of_Anguilla.svg/500px-Flag_of_Anguilla.svg.png", // Anguilla
        "AL" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/36/Flag_of_Albania.svg/500px-Flag_of_Albania.svg.png", // Albania
        "AM" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/2f/Flag_of_Armenia.svg/500px-Flag_of_Armenia.svg.png", // Armenia
        "AO" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9d/Flag_of_Angola.svg/500px-Flag_of_Angola.svg.png", // Angola
        "AR" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1a/Flag_of_Argentina.svg/500px-Flag_of_Argentina.svg.png", // Argentina
        "AS" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/87/Flag_of_American_Samoa.svg/500px-Flag_of_American_Samoa.svg.png", // American Samoa
        "AT" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/41/Flag_of_Austria.svg/500px-Flag_of_Austria.svg.png", // Austria
        "AU" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/88/Flag_of_Australia_%28converted%29.svg/500px-Flag_of_Australia_%28converted%29.svg.png", // Australia
        "AW" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/f6/Flag_of_Aruba.svg/500px-Flag_of_Aruba.svg.png", // Aruba
        "AX" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/52/Flag_of_%C3%85land.svg/500px-Flag_of_%C3%85land.svg.png", // Åland
        "AZ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/dd/Flag_of_Azerbaijan.svg/500px-Flag_of_Azerbaijan.svg.png", // Azerbaijan
        "BA" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/bf/Flag_of_Bosnia_and_Herzegovina.svg/500px-Flag_of_Bosnia_and_Herzegovina.svg.png", // Bosnia and Herzegovina
        "BB" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/ef/Flag_of_Barbados.svg/500px-Flag_of_Barbados.svg.png", // Barbados
        "BD" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/f9/Flag_of_Bangladesh.svg/500px-Flag_of_Bangladesh.svg.png", // Bangladesh
        "BE" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/92/Flag_of_Belgium_%28civil%29.svg/500px-Flag_of_Belgium_%28civil%29.svg.png", // Belgium
        "BF" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/31/Flag_of_Burkina_Faso.svg/500px-Flag_of_Burkina_Faso.svg.png", // Burkina Faso
        "BG" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9a/Flag_of_Bulgaria.svg/500px-Flag_of_Bulgaria.svg.png", // Bulgaria
        "BH" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/2c/Flag_of_Bahrain.svg/500px-Flag_of_Bahrain.svg.png", // Bahrain
        "BI" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/50/Flag_of_Burundi.svg/500px-Flag_of_Burundi.svg.png", // Burundi
        "BJ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0a/Flag_of_Benin.svg/500px-Flag_of_Benin.svg.png", // Benin
        "BL" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c3/Flag_of_France.svg/500px-Flag_of_France.svg.png", // Saint Barthélemy
        "BM" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/bf/Flag_of_Bermuda.svg/500px-Flag_of_Bermuda.svg.png", // Bermuda
        "BN" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9c/Flag_of_Brunei.svg/500px-Flag_of_Brunei.svg.png", // Brunei
        "BO" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/48/Flag_of_Bolivia.svg/500px-Flag_of_Bolivia.svg.png", // Bolivia
        "BQ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/20/Flag_of_the_Netherlands.svg/500px-Flag_of_the_Netherlands.svg.png", // Caribbean Netherlands
        "BR" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/05/Flag_of_Brazil.svg/500px-Flag_of_Brazil.svg.png", // Brazil
        "BS" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/93/Flag_of_the_Bahamas.svg/500px-Flag_of_the_Bahamas.svg.png", // The Bahamas
        "BT" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/91/Flag_of_Bhutan.svg/500px-Flag_of_Bhutan.svg.png", // Bhutan
        "BV" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d9/Flag_of_Norway.svg/500px-Flag_of_Norway.svg.png", // Bouvet Island
        "BW" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fa/Flag_of_Botswana.svg/500px-Flag_of_Botswana.svg.png", // Botswana
        "BY" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/85/Flag_of_Belarus.svg/500px-Flag_of_Belarus.svg.png", // Belarus
        "BZ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e7/Flag_of_Belize.svg/500px-Flag_of_Belize.svg.png", // Belize
        "CA" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d9/Flag_of_Canada_%28Pantone%29.svg/500px-Flag_of_Canada_%28Pantone%29.svg.png", // Canada
        "CC" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/88/Flag_of_Australia_%28converted%29.svg/500px-Flag_of_Australia_%28converted%29.svg.png", // Cocos (Keeling) Islands
        "CD" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6f/Flag_of_the_Democratic_Republic_of_the_Congo.svg/500px-Flag_of_the_Democratic_Republic_of_the_Congo.svg.png", // Democratic Republic of the Congo
        "CF" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6f/Flag_of_the_Central_African_Republic.svg/500px-Flag_of_the_Central_African_Republic.svg.png", // Central African Republic
        "CG" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/92/Flag_of_the_Republic_of_the_Congo.svg/500px-Flag_of_the_Republic_of_the_Congo.svg.png", // Republic of the Congo
        "CH" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/08/Flag_of_Switzerland_%28Pantone%29.svg/500px-Flag_of_Switzerland_%28Pantone%29.svg.png", // Switzerland
        "CI" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fe/Flag_of_C%C3%B4te_d%27Ivoire.svg/500px-Flag_of_C%C3%B4te_d%27Ivoire.svg.png", // Ivory Coast
        "CK" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/35/Flag_of_the_Cook_Islands.svg/500px-Flag_of_the_Cook_Islands.svg.png", // Cook Islands
        "CL" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/78/Flag_of_Chile.svg/500px-Flag_of_Chile.svg.png", // Chile
        "CM" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4f/Flag_of_Cameroon.svg/500px-Flag_of_Cameroon.svg.png", // Cameroon
        "CN" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fa/Flag_of_the_People%27s_Republic_of_China.svg/500px-Flag_of_the_People%27s_Republic_of_China.svg.png", // People's Republic of China
        "CO" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/21/Flag_of_Colombia.svg/500px-Flag_of_Colombia.svg.png", // Colombia
        "CQ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/64/Flag_of_Sark.svg/500px-Flag_of_Sark.svg.png", // Sark
        "CR" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/f2/Flag_of_Costa_Rica.svg/500px-Flag_of_Costa_Rica.svg.png", // Costa Rica
        "CU" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/bd/Flag_of_Cuba.svg/500px-Flag_of_Cuba.svg.png", // Cuba
        "CV" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/65/Flag_of_Cape_Verde_%2810-17%29.svg/500px-Flag_of_Cape_Verde_%2810-17%29.svg.png", // Cape Verde
        "CW" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b1/Flag_of_Cura%C3%A7ao.svg/500px-Flag_of_Cura%C3%A7ao.svg.png", // Curaçao
        "CX" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/67/Flag_of_Christmas_Island.svg/500px-Flag_of_Christmas_Island.svg.png", // Christmas Island
        "CY" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d4/Flag_of_Cyprus.svg/500px-Flag_of_Cyprus.svg.png", // Cyprus
        "CZ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/cb/Flag_of_the_Czech_Republic.svg/500px-Flag_of_the_Czech_Republic.svg.png", // Czech Republic
        "DE" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/ba/Flag_of_Germany.svg/500px-Flag_of_Germany.svg.png", // Germany
        "DJ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/34/Flag_of_Djibouti.svg/500px-Flag_of_Djibouti.svg.png", // Djibouti
        "DK" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9c/Flag_of_Denmark.svg/500px-Flag_of_Denmark.svg.png", // Denmark
        "DM" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c4/Flag_of_Dominica.svg/500px-Flag_of_Dominica.svg.png", // Dominica
        "DO" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9f/Flag_of_the_Dominican_Republic.svg/500px-Flag_of_the_Dominican_Republic.svg.png", // Dominican Republic
        "DZ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/77/Flag_of_Algeria.svg/500px-Flag_of_Algeria.svg.png", // Algeria
        "EC" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e8/Flag_of_Ecuador.svg/500px-Flag_of_Ecuador.svg.png", // Ecuador
        "EE" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/8f/Flag_of_Estonia.svg/500px-Flag_of_Estonia.svg.png", // Estonia
        "EG" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fe/Flag_of_Egypt.svg/500px-Flag_of_Egypt.svg.png", // Egypt
        "EH" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/26/Flag_of_the_Sahrawi_Arab_Democratic_Republic.svg/500px-Flag_of_the_Sahrawi_Arab_Democratic_Republic.svg.png", // Western Sahara
        "ER" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/29/Flag_of_Eritrea.svg/500px-Flag_of_Eritrea.svg.png", // Eritrea
        "ES" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9a/Flag_of_Spain.svg/500px-Flag_of_Spain.svg.png", // Spain
        "ET" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/71/Flag_of_Ethiopia.svg/500px-Flag_of_Ethiopia.svg.png", // Ethiopia
        "FI" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/bc/Flag_of_Finland.svg/500px-Flag_of_Finland.svg.png", // Finland
        "FJ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/ba/Flag_of_Fiji.svg/500px-Flag_of_Fiji.svg.png", // Fiji
        "FK" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/83/Flag_of_the_Falkland_Islands.svg/500px-Flag_of_the_Falkland_Islands.svg.png", // Falkland Islands
        "FM" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e4/Flag_of_the_Federated_States_of_Micronesia.svg/500px-Flag_of_the_Federated_States_of_Micronesia.svg.png", // Federated States of Micronesia
        "FO" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/3c/Flag_of_the_Faroe_Islands.svg/500px-Flag_of_the_Faroe_Islands.svg.png", // Faroe Islands
        "FR" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c3/Flag_of_France.svg/500px-Flag_of_France.svg.png", // France
        "GA" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/04/Flag_of_Gabon.svg/500px-Flag_of_Gabon.svg.png", // Gabon
        "GB" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/83/Flag_of_the_United_Kingdom_%283-5%29.svg/500px-Flag_of_the_United_Kingdom_%283-5%29.svg.png", // United Kingdom
        "GD" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/bc/Flag_of_Grenada.svg/500px-Flag_of_Grenada.svg.png", // Grenada
        "GE" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0f/Flag_of_Georgia.svg/500px-Flag_of_Georgia.svg.png", // Georgia
        "GF" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c3/Flag_of_France.svg/500px-Flag_of_France.svg.png", // French Guiana
        "GG" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fa/Flag_of_Guernsey.svg/500px-Flag_of_Guernsey.svg.png", // Guernsey
        "GH" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/19/Flag_of_Ghana.svg/500px-Flag_of_Ghana.svg.png", // Ghana
        "GI" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/02/Flag_of_Gibraltar.svg/500px-Flag_of_Gibraltar.svg.png", // Gibraltar
        "GL" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/09/Flag_of_Greenland.svg/500px-Flag_of_Greenland.svg.png", // Greenland
        "GM" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/77/Flag_of_The_Gambia.svg/500px-Flag_of_The_Gambia.svg.png", // The Gambia
        "GN" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/ed/Flag_of_Guinea.svg/500px-Flag_of_Guinea.svg.png", // Guinea
        "GP" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c3/Flag_of_France.svg/500px-Flag_of_France.svg.png", // Guadeloupe
        "GQ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/31/Flag_of_Equatorial_Guinea.svg/500px-Flag_of_Equatorial_Guinea.svg.png", // Equatorial Guinea
        "GR" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/5c/Flag_of_Greece.svg/500px-Flag_of_Greece.svg.png", // Greece
        "GS" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/ed/Flag_of_South_Georgia_and_the_South_Sandwich_Islands.svg/500px-Flag_of_South_Georgia_and_the_South_Sandwich_Islands.svg.png", // South Georgia and the South Sandwich Islands
        "GT" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/ec/Flag_of_Guatemala.svg/500px-Flag_of_Guatemala.svg.png", // Guatemala
        "GU" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/07/Flag_of_Guam.svg/500px-Flag_of_Guam.svg.png", // Guam
        "GW" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/01/Flag_of_Guinea-Bissau.svg/500px-Flag_of_Guinea-Bissau.svg.png", // Guinea-Bissau
        "GY" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/99/Flag_of_Guyana.svg/500px-Flag_of_Guyana.svg.png", // Guyana
        "HK" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/5b/Flag_of_Hong_Kong.svg/500px-Flag_of_Hong_Kong.svg.png", // Hong Kong
        "HM" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/88/Flag_of_Australia_%28converted%29.svg/500px-Flag_of_Australia_%28converted%29.svg.png", // Heard Island and McDonald Islands
        "HN" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/79/Flag_of_Honduras_%281949%E2%80%932022%2C_2026%E2%80%93present%29.svg/500px-Flag_of_Honduras_%281949%E2%80%932022%2C_2026%E2%80%93present%29.svg.png", // Honduras
        "HR" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1b/Flag_of_Croatia.svg/500px-Flag_of_Croatia.svg.png", // Croatia
        "HT" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/56/Flag_of_Haiti.svg/500px-Flag_of_Haiti.svg.png", // Haiti
        "HU" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c1/Flag_of_Hungary.svg/500px-Flag_of_Hungary.svg.png", // Hungary
        "ID" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9f/Flag_of_Indonesia.svg/500px-Flag_of_Indonesia.svg.png", // Indonesia
        "IE" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/45/Flag_of_Ireland.svg/500px-Flag_of_Ireland.svg.png", // Ireland
        "IL" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d4/Flag_of_Israel.svg/500px-Flag_of_Israel.svg.png", // Israel
        "IM" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/bc/Flag_of_the_Isle_of_Man.svg/500px-Flag_of_the_Isle_of_Man.svg.png", // Isle of Man
        "IN" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/41/Flag_of_India.svg/500px-Flag_of_India.svg.png", // India
        "IO" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/12/Flag_of_the_British_Indian_Ocean_Territory_2025.svg/500px-Flag_of_the_British_Indian_Ocean_Territory_2025.svg.png", // British Indian Ocean Territory
        "IQ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/f6/Flag_of_Iraq.svg/500px-Flag_of_Iraq.svg.png", // Iraq
        "IR" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/ca/Flag_of_Iran.svg/500px-Flag_of_Iran.svg.png", // Iran
        "IS" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/ce/Flag_of_Iceland.svg/500px-Flag_of_Iceland.svg.png", // Iceland
        "IT" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/03/Flag_of_Italy.svg/500px-Flag_of_Italy.svg.png", // Italy
        "JE" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1c/Flag_of_Jersey.svg/500px-Flag_of_Jersey.svg.png", // Jersey
        "JM" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0a/Flag_of_Jamaica.svg/500px-Flag_of_Jamaica.svg.png", // Jamaica
        "JO" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c0/Flag_of_Jordan.svg/500px-Flag_of_Jordan.svg.png", // Jordan
        "JP" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9e/Flag_of_Japan.svg/500px-Flag_of_Japan.svg.png", // Japan
        "KE" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/49/Flag_of_Kenya.svg/500px-Flag_of_Kenya.svg.png", // Kenya
        "KG" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c7/Flag_of_Kyrgyzstan.svg/500px-Flag_of_Kyrgyzstan.svg.png", // Kyrgyzstan
        "KH" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/83/Flag_of_Cambodia.svg/500px-Flag_of_Cambodia.svg.png", // Cambodia
        "KI" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d3/Flag_of_Kiribati.svg/500px-Flag_of_Kiribati.svg.png", // Kiribati
        "KM" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/94/Flag_of_the_Comoros.svg/500px-Flag_of_the_Comoros.svg.png", // Comoros
        "KN" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fe/Flag_of_Saint_Kitts_and_Nevis.svg/500px-Flag_of_Saint_Kitts_and_Nevis.svg.png", // Saint Kitts and Nevis
        "KP" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/51/Flag_of_North_Korea.svg/500px-Flag_of_North_Korea.svg.png", // North Korea
        "KR" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/09/Flag_of_South_Korea.svg/500px-Flag_of_South_Korea.svg.png", // South Korea
        "KW" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/aa/Flag_of_Kuwait.svg/500px-Flag_of_Kuwait.svg.png", // Kuwait
        "KY" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0f/Flag_of_the_Cayman_Islands.svg/500px-Flag_of_the_Cayman_Islands.svg.png", // Cayman Islands
        "KZ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d3/Flag_of_Kazakhstan.svg/500px-Flag_of_Kazakhstan.svg.png", // Kazakhstan
        "LA" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/56/Flag_of_Laos.svg/500px-Flag_of_Laos.svg.png", // Laos
        "LB" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/59/Flag_of_Lebanon.svg/500px-Flag_of_Lebanon.svg.png", // Lebanon
        "LC" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9f/Flag_of_Saint_Lucia.svg/500px-Flag_of_Saint_Lucia.svg.png", // Saint Lucia
        "LI" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/47/Flag_of_Liechtenstein.svg/500px-Flag_of_Liechtenstein.svg.png", // Liechtenstein
        "LK" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/11/Flag_of_Sri_Lanka.svg/500px-Flag_of_Sri_Lanka.svg.png", // Sri Lanka
        "LR" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b8/Flag_of_Liberia.svg/500px-Flag_of_Liberia.svg.png", // Liberia
        "LS" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4a/Flag_of_Lesotho.svg/500px-Flag_of_Lesotho.svg.png", // Lesotho
        "LT" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/11/Flag_of_Lithuania.svg/500px-Flag_of_Lithuania.svg.png", // Lithuania
        "LU" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/da/Flag_of_Luxembourg.svg/500px-Flag_of_Luxembourg.svg.png", // Luxembourg
        "LV" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/84/Flag_of_Latvia.svg/500px-Flag_of_Latvia.svg.png", // Latvia
        "LY" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/05/Flag_of_Libya.svg/500px-Flag_of_Libya.svg.png", // Libya
        "MA" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/2c/Flag_of_Morocco.svg/500px-Flag_of_Morocco.svg.png", // Morocco
        "MC" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/ea/Flag_of_Monaco.svg/500px-Flag_of_Monaco.svg.png", // Monaco
        "MD" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/27/Flag_of_Moldova.svg/500px-Flag_of_Moldova.svg.png", // Moldova
        "ME" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/64/Flag_of_Montenegro.svg/500px-Flag_of_Montenegro.svg.png", // Montenegro
        "MF" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c3/Flag_of_France.svg/500px-Flag_of_France.svg.png", // Saint-Martin
        "MG" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/bc/Flag_of_Madagascar.svg/500px-Flag_of_Madagascar.svg.png", // Madagascar
        "MH" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/2e/Flag_of_the_Marshall_Islands.svg/500px-Flag_of_the_Marshall_Islands.svg.png", // Marshall Islands
        "MK" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/79/Flag_of_North_Macedonia.svg/500px-Flag_of_North_Macedonia.svg.png", // North Macedonia
        "ML" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/92/Flag_of_Mali.svg/500px-Flag_of_Mali.svg.png", // Mali
        "MM" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/8c/Flag_of_Myanmar.svg/500px-Flag_of_Myanmar.svg.png", // Myanmar
        "MN" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4c/Flag_of_Mongolia.svg/500px-Flag_of_Mongolia.svg.png", // Mongolia
        "MO" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/63/Flag_of_Macau.svg/500px-Flag_of_Macau.svg.png", // Macau
        "MP" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e0/Flag_of_the_Northern_Mariana_Islands.svg/500px-Flag_of_the_Northern_Mariana_Islands.svg.png", // Northern Mariana Islands
        "MQ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/27/Flag-of-Martinique.svg/500px-Flag-of-Martinique.svg.png", // Martinique
        "MR" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/43/Flag_of_Mauritania.svg/500px-Flag_of_Mauritania.svg.png", // Mauritania
        "MS" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d0/Flag_of_Montserrat.svg/500px-Flag_of_Montserrat.svg.png", // Montserrat
        "MT" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/73/Flag_of_Malta.svg/500px-Flag_of_Malta.svg.png", // Malta
        "MU" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/77/Flag_of_Mauritius.svg/500px-Flag_of_Mauritius.svg.png", // Mauritius
        "MV" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0f/Flag_of_Maldives.svg/500px-Flag_of_Maldives.svg.png", // Maldives
        "MW" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d1/Flag_of_Malawi.svg/500px-Flag_of_Malawi.svg.png", // Malawi
        "MX" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fc/Flag_of_Mexico.svg/500px-Flag_of_Mexico.svg.png", // Mexico
        "MY" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/66/Flag_of_Malaysia.svg/500px-Flag_of_Malaysia.svg.png", // Malaysia
        "MZ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d0/Flag_of_Mozambique.svg/500px-Flag_of_Mozambique.svg.png", // Mozambique
        "NA" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/00/Flag_of_Namibia.svg/500px-Flag_of_Namibia.svg.png", // Namibia
        "NC" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/26/Flags_of_New_Caledonia.svg/500px-Flags_of_New_Caledonia.svg.png", // New Caledonia
        "NE" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/f4/Flag_of_Niger.svg/500px-Flag_of_Niger.svg.png", // Niger
        "NF" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/48/Flag_of_Norfolk_Island.svg/500px-Flag_of_Norfolk_Island.svg.png", // Norfolk Island
        "NG" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/79/Flag_of_Nigeria.svg/500px-Flag_of_Nigeria.svg.png", // Nigeria
        "NI" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/19/Flag_of_Nicaragua.svg/500px-Flag_of_Nicaragua.svg.png", // Nicaragua
        "NL" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/20/Flag_of_the_Netherlands.svg/500px-Flag_of_the_Netherlands.svg.png", // Kingdom of the Netherlands
        "NO" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d9/Flag_of_Norway.svg/500px-Flag_of_Norway.svg.png", // Norway
        "NP" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9b/Flag_of_Nepal.svg/500px-Flag_of_Nepal.svg.png", // Nepal
        "NR" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/30/Flag_of_Nauru.svg/500px-Flag_of_Nauru.svg.png", // Nauru
        "NU" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/01/Flag_of_Niue.svg/500px-Flag_of_Niue.svg.png", // Niue
        "NZ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/3e/Flag_of_New_Zealand.svg/500px-Flag_of_New_Zealand.svg.png", // New Zealand
        "OM" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/dd/Flag_of_Oman.svg/500px-Flag_of_Oman.svg.png", // Oman
        "PA" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/ab/Flag_of_Panama.svg/500px-Flag_of_Panama.svg.png", // Panama
        "PE" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/cf/Flag_of_Peru.svg/500px-Flag_of_Peru.svg.png", // Peru
        "PF" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/db/Flag_of_French_Polynesia.svg/500px-Flag_of_French_Polynesia.svg.png", // French Polynesia
        "PG" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e3/Flag_of_Papua_New_Guinea.svg/500px-Flag_of_Papua_New_Guinea.svg.png", // Papua New Guinea
        "PH" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/99/Flag_of_the_Philippines.svg/500px-Flag_of_the_Philippines.svg.png", // Philippines
        "PK" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/32/Flag_of_Pakistan.svg/500px-Flag_of_Pakistan.svg.png", // Pakistan
        "PL" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/12/Flag_of_Poland.svg/500px-Flag_of_Poland.svg.png", // Poland
        "PM" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c3/Flag_of_France.svg/500px-Flag_of_France.svg.png", // Saint Pierre and Miquelon
        "PN" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/88/Flag_of_the_Pitcairn_Islands.svg/500px-Flag_of_the_Pitcairn_Islands.svg.png", // Pitcairn Islands
        "PR" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/28/Flag_of_Puerto_Rico.svg/500px-Flag_of_Puerto_Rico.svg.png", // Puerto Rico
        "PS" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/00/Flag_of_Palestine.svg/500px-Flag_of_Palestine.svg.png", // Palestine
        "PT" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a8/Flag_of_Portugal_%28official%29.svg/500px-Flag_of_Portugal_%28official%29.svg.png", // Portugal
        "PW" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/48/Flag_of_Palau.svg/500px-Flag_of_Palau.svg.png", // Palau
        "PY" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/27/Flag_of_Paraguay.svg/500px-Flag_of_Paraguay.svg.png", // Paraguay
        "QA" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/65/Flag_of_Qatar.svg/500px-Flag_of_Qatar.svg.png", // Qatar
        "RE" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c3/Flag_of_France.svg/500px-Flag_of_France.svg.png", // Réunion
        "RO" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/73/Flag_of_Romania.svg/500px-Flag_of_Romania.svg.png", // Romania
        "RS" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/ff/Flag_of_Serbia.svg/500px-Flag_of_Serbia.svg.png", // Serbia
        "RU" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/f3/Flag_of_Russia.svg/500px-Flag_of_Russia.svg.png", // Russia
        "RW" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/17/Flag_of_Rwanda.svg/500px-Flag_of_Rwanda.svg.png", // Rwanda
        "SA" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0d/Flag_of_Saudi_Arabia.svg/500px-Flag_of_Saudi_Arabia.svg.png", // Saudi Arabia
        "SB" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/74/Flag_of_the_Solomon_Islands.svg/500px-Flag_of_the_Solomon_Islands.svg.png", // Solomon Islands
        "SC" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fc/Flag_of_Seychelles.svg/500px-Flag_of_Seychelles.svg.png", // Seychelles
        "SD" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/01/Flag_of_Sudan.svg/500px-Flag_of_Sudan.svg.png", // Sudan
        "SE" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4c/Flag_of_Sweden.svg/500px-Flag_of_Sweden.svg.png", // Sweden
        "SG" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/48/Flag_of_Singapore.svg/500px-Flag_of_Singapore.svg.png", // Singapore
        "SH" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a5/Flag_of_the_United_Kingdom_%281-2%29.svg/500px-Flag_of_the_United_Kingdom_%281-2%29.svg.png", // Saint Helena, Ascension and Tristan da Cunha
        "SI" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/f0/Flag_of_Slovenia.svg/500px-Flag_of_Slovenia.svg.png", // Slovenia
        "SJ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d9/Flag_of_Norway.svg/500px-Flag_of_Norway.svg.png", // Svalbard and Jan Mayen
        "SK" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e6/Flag_of_Slovakia.svg/500px-Flag_of_Slovakia.svg.png", // Slovakia
        "SL" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/17/Flag_of_Sierra_Leone.svg/500px-Flag_of_Sierra_Leone.svg.png", // Sierra Leone
        "SM" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b1/Flag_of_San_Marino.svg/500px-Flag_of_San_Marino.svg.png", // San Marino
        "SN" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fd/Flag_of_Senegal.svg/500px-Flag_of_Senegal.svg.png", // Senegal
        "SO" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a0/Flag_of_Somalia.svg/500px-Flag_of_Somalia.svg.png", // Somalia
        "SR" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/60/Flag_of_Suriname.svg/500px-Flag_of_Suriname.svg.png", // Suriname
        "SS" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/7a/Flag_of_South_Sudan.svg/500px-Flag_of_South_Sudan.svg.png", // South Sudan
        "ST" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0a/Flag_of_S%C3%A3o_Tom%C3%A9_and_Pr%C3%ADncipe.svg/500px-Flag_of_S%C3%A3o_Tom%C3%A9_and_Pr%C3%ADncipe.svg.png", // São Tomé and Príncipe
        "SV" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/34/Flag_of_El_Salvador.svg/500px-Flag_of_El_Salvador.svg.png", // El Salvador
        "SX" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d3/Flag_of_Sint_Maarten.svg/500px-Flag_of_Sint_Maarten.svg.png", // Sint Maarten
        "SY" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/54/Flag_of_Syria_%282025-%29.svg/500px-Flag_of_Syria_%282025-%29.svg.png", // Syria
        "SZ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fb/Flag_of_Eswatini.svg/500px-Flag_of_Eswatini.svg.png", // Eswatini
        "TC" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a0/Flag_of_the_Turks_and_Caicos_Islands.svg/500px-Flag_of_the_Turks_and_Caicos_Islands.svg.png", // Turks and Caicos Islands
        "TD" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4b/Flag_of_Chad.svg/500px-Flag_of_Chad.svg.png", // Chad
        "TF" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a7/Flag_of_the_French_Southern_and_Antarctic_Lands.svg/500px-Flag_of_the_French_Southern_and_Antarctic_Lands.svg.png", // French Southern and Antarctic Lands
        "TG" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/7a/Flag_of_Togo_%283-2%29.svg/500px-Flag_of_Togo_%283-2%29.svg.png", // Togo
        "TH" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a9/Flag_of_Thailand.svg/500px-Flag_of_Thailand.svg.png", // Thailand
        "TJ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d0/Flag_of_Tajikistan.svg/500px-Flag_of_Tajikistan.svg.png", // Tajikistan
        "TK" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/8e/Flag_of_Tokelau.svg/500px-Flag_of_Tokelau.svg.png", // Tokelau
        "TL" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/26/Flag_of_East_Timor.svg/500px-Flag_of_East_Timor.svg.png", // Timor-Leste
        "TM" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1b/Flag_of_Turkmenistan.svg/500px-Flag_of_Turkmenistan.svg.png", // Turkmenistan
        "TN" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/ce/Flag_of_Tunisia.svg/500px-Flag_of_Tunisia.svg.png", // Tunisia
        "TO" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9a/Flag_of_Tonga.svg/500px-Flag_of_Tonga.svg.png", // Tonga
        "TR" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b4/Flag_of_Turkey.svg/500px-Flag_of_Turkey.svg.png", // Turkey
        "TT" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/64/Flag_of_Trinidad_and_Tobago.svg/500px-Flag_of_Trinidad_and_Tobago.svg.png", // Trinidad and Tobago
        "TV" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/38/Flag_of_Tuvalu.svg/500px-Flag_of_Tuvalu.svg.png", // Tuvalu
        "TW" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/72/Flag_of_the_Republic_of_China.svg/500px-Flag_of_the_Republic_of_China.svg.png", // Taiwan
        "TZ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/38/Flag_of_Tanzania.svg/500px-Flag_of_Tanzania.svg.png", // Tanzania
        "UA" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/49/Flag_of_Ukraine.svg/500px-Flag_of_Ukraine.svg.png", // Ukraine
        "UG" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4e/Flag_of_Uganda.svg/500px-Flag_of_Uganda.svg.png", // Uganda
        "UM" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/96/Flag_of_the_United_States_%28DDD-F-416E_specifications%29.svg/500px-Flag_of_the_United_States_%28DDD-F-416E_specifications%29.svg.png", // United States Minor Outlying Islands
        "US" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a4/Flag_of_the_United_States.svg/500px-Flag_of_the_United_States.svg.png", // United States
        "UY" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fe/Flag_of_Uruguay.svg/500px-Flag_of_Uruguay.svg.png", // Uruguay
        "UZ" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/84/Flag_of_Uzbekistan.svg/500px-Flag_of_Uzbekistan.svg.png", // Uzbekistan
        "VA" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b3/Flag_of_Vatican_City_%282023%E2%80%93present%29.svg/500px-Flag_of_Vatican_City_%282023%E2%80%93present%29.svg.png", // Vatican City
        "VC" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6d/Flag_of_Saint_Vincent_and_the_Grenadines.svg/500px-Flag_of_Saint_Vincent_and_the_Grenadines.svg.png", // Saint Vincent and the Grenadines
        "VE" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/06/Flag_of_Venezuela.svg/500px-Flag_of_Venezuela.svg.png", // Venezuela
        "VG" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/42/Flag_of_the_British_Virgin_Islands.svg/500px-Flag_of_the_British_Virgin_Islands.svg.png", // British Virgin Islands
        "VI" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/f8/Flag_of_the_United_States_Virgin_Islands.svg/500px-Flag_of_the_United_States_Virgin_Islands.svg.png", // United States Virgin Islands
        "VN" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/21/Flag_of_Vietnam.svg/500px-Flag_of_Vietnam.svg.png", // Vietnam
        "VU" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/bc/Flag_of_Vanuatu.svg/500px-Flag_of_Vanuatu.svg.png", // Vanuatu
        "WF" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c3/Flag_of_France.svg/500px-Flag_of_France.svg.png", // Wallis and Futuna
        "WS" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/31/Flag_of_Samoa.svg/500px-Flag_of_Samoa.svg.png", // Samoa
        "XK" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1f/Flag_of_Kosovo.svg/500px-Flag_of_Kosovo.svg.png", // Kosovo
        "YE" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/89/Flag_of_Yemen.svg/500px-Flag_of_Yemen.svg.png", // Yemen
        "YT" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c3/Flag_of_France.svg/500px-Flag_of_France.svg.png", // Mayotte
        "ZA" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/af/Flag_of_South_Africa.svg/500px-Flag_of_South_Africa.svg.png", // South Africa
        "ZM" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/06/Flag_of_Zambia.svg/500px-Flag_of_Zambia.svg.png", // Zambia
        "ZW" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6a/Flag_of_Zimbabwe.svg/500px-Flag_of_Zimbabwe.svg.png", // Zimbabwe
    )

    /** Returns the flag URL for [countryCode] (e.g. "us" or "US"), or null if unknown. */
    fun urlFor(countryCode: String): String? = urls[countryCode.uppercase()]
}
