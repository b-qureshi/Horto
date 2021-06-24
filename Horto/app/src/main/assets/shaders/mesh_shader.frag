#version 300 es
/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Shader file updated - Bilal Qureshi, 15/04/2021
 */

in vec3 eyePosition;
in vec3 eyeNorm;
in vec2 texCoord;
in vec3 vColour;

layout(location = 0) out vec4 o_FragColor;

void main() {
  vec3 vtexCoord = vec3(texCoord.x, texCoord.y, 1.0f);
  o_FragColor = vec4(vtexCoord * vColour, 1.0);
}
