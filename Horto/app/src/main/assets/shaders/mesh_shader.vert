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
 * Shader file updated to include different lighting model - Bilal Qureshi, 15/04/2021
 *  - Phong Lighting model created
 */

uniform mat4 u_ModelView;
uniform mat4 u_ModelViewProjection;

layout(location = 0) in vec4 pos;
layout(location = 1) in vec2 tex;
layout(location = 2) in vec3 normal;

out vec3 eyePos;
out vec3 eyeNorm;
out vec2 texCoord;
out vec3 vColour;


// Phong Model - Used from my Computer Science Graphics Coursework
vec3 PhongModel(vec3 eyePosition, vec3 eyeNormal)
{
  // Light Qualities
  vec3 lightPosition = vec3(100, 1000, -100);
  vec3 la = vec3(0.5f);
  vec3 ld = vec3(0.6f);
  vec3 ls = vec3(0.2f);

  // Material Properties
  vec3 ma = vec3(1.0f);
  vec3 md = vec3(0.0f);
  vec3 ms = vec3(1.0f);
  vec3 shininess = vec3(15.0f);

  // Phong Lighting Model
  vec3 s = normalize(vec3(lightPosition - eyePosition));
  vec3 v = normalize(-eyePosition.xyz);
  vec3 r = reflect(-s, eyeNormal);
  vec3 n = eyeNormal;
  vec3 ambient = la * ma;
  float sDotN = max(dot(s, n), 0.0f);
  vec3 diffuse = ld * md * sDotN;
  vec3 specular = vec3(0.0f);
  float eps = 0.000001f; // add eps to shininess below -- pow not defined if second argument is 0 (as described in GLSL documentation)
  if (sDotN > 0.0f)
  specular = ls * ms * max(dot(r, v), 0.0f), shininess + eps;
  return ambient + diffuse + specular * 20.0f;
}

void main() {
  eyePos = (u_ModelView * pos).xyz;
  eyeNorm = normalize((u_ModelView * vec4(normal, 0.0)).xyz);
  texCoord = tex;
  gl_Position = u_ModelViewProjection * pos;
  vColour = PhongModel(eyePos, eyeNorm);
}
