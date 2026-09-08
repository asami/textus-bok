#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd -P)"
CONFIGURE_SCRIPT="$PROJECT_ROOT/scripts/configure-simplemodeling-development.sh"
SNAPSHOT_SCRIPT="$SCRIPT_DIR/prepare-simplemodeling-wip-snapshot.sh"
PROBE_SCRIPT="$SCRIPT_DIR/probe-managed-bok.py"
README_PATH="$PROJECT_ROOT/README.md"
DEVELOPER_GUIDE_PATH="$PROJECT_ROOT/docs/developer-guide.md"
CONTRACT_PATH="$PROJECT_ROOT/docs/spec/bok-startup-bootstrap-contract.md"
USER_GUIDE_PATH="$PROJECT_ROOT/src/main/car/manual/user-guide.md"
RUNTIME_ROOT="$(mktemp -d /private/tmp/textus-bok-managed-bootstrap-tools.XXXXXX)"

cleanup() {
  local status=$?
  case "$RUNTIME_ROOT" in
    /private/tmp/textus-bok-managed-bootstrap-tools.??????)
      rm -rf -- "$RUNTIME_ROOT"
      ;;
    *)
      echo "Refusing to remove an unexpected managed-bootstrap check directory: $RUNTIME_ROOT" >&2
      status=1
      ;;
  esac
  exit "$status"
}
trap cleanup EXIT

for script in \
  "$CONFIGURE_SCRIPT" \
  "$SNAPSHOT_SCRIPT" \
  "$PROBE_SCRIPT" \
  "$SCRIPT_DIR/check-script-entrypoint-layout.sh" \
  "$SCRIPT_DIR/check-managed-bootstrap-tools.sh"; do
  [[ -f "$script" ]] || { echo "Managed bootstrap tool is missing: $script" >&2; exit 1; }
done

[[ -f "$README_PATH" ]] || { echo "README is missing: $README_PATH" >&2; exit 1; }
[[ -f "$DEVELOPER_GUIDE_PATH" ]] || { echo "developer guide is missing: $DEVELOPER_GUIDE_PATH" >&2; exit 1; }
[[ -f "$CONTRACT_PATH" ]] || { echo "startup contract is missing: $CONTRACT_PATH" >&2; exit 1; }
[[ -f "$USER_GUIDE_PATH" ]] || { echo "user guide is missing: $USER_GUIDE_PATH" >&2; exit 1; }
! rg -F -- '1.0.57' "$README_PATH" >/dev/null
! rg -F -- '1.0.57' "$USER_GUIDE_PATH" >/dev/null

for document in "$README_PATH" "$DEVELOPER_GUIDE_PATH" "$CONTRACT_PATH" "$USER_GUIDE_PATH"; do
  ! rg -F -- '/private/tmp/textus-bok-simplemodeling-wip.XXXXXX/website.d' "$document" >/dev/null
  ! rg -F -- '--present-term-id architecture:component' "$document" >/dev/null
  ! rg -F -- '--absent-term-id p8-c:not-present' "$document" >/dev/null
  ! rg -F -- '--expected-term-count 123' "$document" >/dev/null
  ! rg -F -- '--generation sha256:<generation-from-config>' "$document" >/dev/null
  rg -F -- '--present-term-id architecture:availability' "$document" >/dev/null
  rg -F -- '--absent-term-id literate-modeling:story' "$document" >/dev/null
  rg -F -- '--expected-term-count 215' "$document" >/dev/null
  rg -F -- 'sha256:40f96e99b2308e0474584513ff7602928008d57de8e3f3bd2494a5aa83402cec' "$document" >/dev/null
  rg -F -- '--expected-term-count 212' "$document" >/dev/null
  rg -F -- 'sha256:5cf33a2697035e3b53c6920446e9c9ce2c94a3c4192d56441a308d5f9b956b5f' "$document" >/dev/null
done
rg -F -- 'P8-C managed-runtime acceptance' "$CONTRACT_PATH" >/dev/null
rg -F -- 'SimpleModeling.org repository' "$CONTRACT_PATH" >/dev/null
rg -F -- 'SimpleModeling.org repository' "$DEVELOPER_GUIDE_PATH" >/dev/null
rg -F -- 'independently cmp-verified byte-identical' "$CONTRACT_PATH" >/dev/null
rg -F -- 'independently cmp-verified byte-identical' "$DEVELOPER_GUIDE_PATH" >/dev/null
rg -F -- '`src/main/website`' "$CONTRACT_PATH" >/dev/null
rg -F -- '`src/main/website`' "$DEVELOPER_GUIDE_PATH" >/dev/null

bash -n "$CONFIGURE_SCRIPT"
bash -n "$SNAPSHOT_SCRIPT"
bash -n "$SCRIPT_DIR/check-script-entrypoint-layout.sh"
bash -n "$SCRIPT_DIR/check-managed-bootstrap-tools.sh"
PYTHONDONTWRITEBYTECODE=1 python3 - "$PROBE_SCRIPT" <<'PY'
import ast
import pathlib
import sys

path = pathlib.Path(sys.argv[1])
ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
PY

python3 - "$RUNTIME_ROOT/website.d" <<'PY'
import json
import pathlib
import sys

root = pathlib.Path(sys.argv[1])
(root / "metadata/cncf").mkdir(parents=True)
(root / "metadata/glossary").mkdir(parents=True)
(root / "metadata/cncf/knowledge-source.json").write_text(
    json.dumps(
        {
            "schemaVersion": "cncf.knowledge-source.v1",
            "kind": "bok-site",
            "sourceRef": {"uri": "https://www.simplemodeling.org/"},
            "resources": [
                {
                    "kind": "glossary-terms",
                    "href": "metadata/glossary/terms.json",
                    "mediaType": "application/json",
                }
            ],
        },
        sort_keys=True,
    ),
    encoding="utf-8",
)
(root / "metadata/glossary/terms.json").write_text(
    json.dumps(
        {
            "terms": [
                {"id": "managed:one", "title": "Managed One"},
                {"id": "managed:two", "title": "Managed Two"},
            ]
        },
        sort_keys=True,
    ),
    encoding="utf-8",
)
PY

config_output="$RUNTIME_ROOT/config.yaml"
"$CONFIGURE_SCRIPT" \
  --source-root "$RUNTIME_ROOT/website.d" \
  --output "$config_output" >/dev/null

python3 - "$config_output" "$RUNTIME_ROOT/website.d" <<'PY'
import pathlib
import re
import sys

config = pathlib.Path(sys.argv[1]).read_text(encoding="utf-8")
source_path = pathlib.Path(sys.argv[2]).resolve()
source_root = str(source_path)
source_root_uri = source_path.as_uri() + "/"
required = (
    "# textus-bok-managed-development-config: v1",
    "textus.bok.profile-registry:",
    "sourceId: \"simplemodeling-development\"",
    "datasetId: \"simplemodeling-bok-development\"",
    "textus.bok.bootstrap:",
    "    - development",
    "rdf-db: in-memory",
    "vector-db: in-memory",
    f'textus.resource.url.file.roots: "{source_root}"',
    f'resource: "{source_root_uri}"',
)
for value in required:
    if value not in config:
        raise SystemExit(f"generated config is missing: {value}")
if "profile: official" in config or "profile: project" in config:
    raise SystemExit("generated config selected a profile other than development")
if re.search(r"generation: \"sha256:[0-9a-f]{64}\"", config) is None:
    raise SystemExit("generated config did not contain a content-bound generation")
PY

python3 - "$RUNTIME_ROOT/unmarked.yaml" <<'PY'
import pathlib
import sys

pathlib.Path(sys.argv[1]).write_text("textus.bok.bootstrap: {}\n", encoding="utf-8")
PY
unmarked_before="$(shasum -a 256 "$RUNTIME_ROOT/unmarked.yaml" | cut -d ' ' -f 1)"
if "$CONFIGURE_SCRIPT" --source-root "$RUNTIME_ROOT/website.d" --output "$RUNTIME_ROOT/unmarked.yaml" >/dev/null 2>&1; then
  echo "unmarked configuration was overwritten" >&2
  exit 1
fi
unmarked_after="$(shasum -a 256 "$RUNTIME_ROOT/unmarked.yaml" | cut -d ' ' -f 1)"
[[ "$unmarked_before" == "$unmarked_after" ]] || {
  echo "unmarked configuration changed after refusal" >&2
  exit 1
}

"$CONFIGURE_SCRIPT" --source-root "$RUNTIME_ROOT/website.d" --output "$config_output" >/dev/null

for invalid_name in missing-manifest unsafe-href malformed-glossary; do
  invalid_root="$RUNTIME_ROOT/$invalid_name/website.d"
  mkdir -p "$invalid_root/metadata/cncf" "$invalid_root/metadata/glossary"
  case "$invalid_name" in
    missing-manifest)
      ;;
    unsafe-href)
      python3 - "$invalid_root" <<'PY'
import json
import pathlib
import sys

root = pathlib.Path(sys.argv[1])
(root / "metadata/cncf/knowledge-source.json").write_text(
    json.dumps({"schemaVersion": "cncf.knowledge-source.v1", "resources": [{"kind": "glossary-terms", "href": "../terms.json"}]}),
    encoding="utf-8",
)
PY
      ;;
    malformed-glossary)
      python3 - "$invalid_root" <<'PY'
import json
import pathlib
import sys

root = pathlib.Path(sys.argv[1])
(root / "metadata/cncf/knowledge-source.json").write_text(
    json.dumps({"schemaVersion": "cncf.knowledge-source.v1", "resources": [{"kind": "glossary-terms", "href": "metadata/glossary/terms.json"}]}),
    encoding="utf-8",
)
(root / "metadata/glossary/terms.json").write_text("not-json", encoding="utf-8")
PY
      ;;
  esac
  if "$CONFIGURE_SCRIPT" --source-root "$invalid_root" --output "$RUNTIME_ROOT/$invalid_name.yaml" >/dev/null 2>&1; then
    echo "invalid KnowledgeSource root was accepted: $invalid_name" >&2
    exit 1
  fi
done

probe_help="$(python3 "$PROBE_SCRIPT" --help)"
for option in \
  --base-url \
  --source-root \
  --present-term-id \
  --absent-term-id \
  --expected-term-count \
  --generation \
  --dataset-id \
  --source-id; do
  [[ "$probe_help" == *"$option"* ]] || { echo "probe help is missing $option" >&2; exit 1; }
done
rg -F -- 'EXPLAIN_TERM = "org.simplemodeling.textus.Bok.BokRetrieval.explainTerm"' "$PROBE_SCRIPT" >/dev/null
rg -F -- '"params": {"name": tool_name, "arguments": arguments}' "$PROBE_SCRIPT" >/dev/null
rg -F -- '        SEARCH_TERMS,' "$PROBE_SCRIPT" >/dev/null
rg -F -- '        EXPLAIN_TERM,' "$PROBE_SCRIPT" >/dev/null
rg -F -- '"term": arguments.absent_term_id, "profile": "development"' "$PROBE_SCRIPT" >/dev/null
rg -F -- 'require("term" not in absent_body and "results" not in absent_body' "$PROBE_SCRIPT" >/dev/null
rg -F -- 'require("result" not in absent_body or absent_body["result"] is None' "$PROBE_SCRIPT" >/dev/null
rg -F -- 'require(isinstance(present_results, list) and len(present_results) == 1' "$PROBE_SCRIPT" >/dev/null
rg -F -- 'class NoRedirectHandler(urllib.request.HTTPRedirectHandler):' "$PROBE_SCRIPT" >/dev/null
rg -F -- 'NO_REDIRECT_OPENER = urllib.request.build_opener(NoRedirectHandler())' "$PROBE_SCRIPT" >/dev/null
rg -F -- 'with NO_REDIRECT_OPENER.open(request, timeout=timeout) as response:' "$PROBE_SCRIPT" >/dev/null
! rg -F -- 'urllib.request.urlopen' "$PROBE_SCRIPT" >/dev/null

PYTHONDONTWRITEBYTECODE=1 python3 - "$PROBE_SCRIPT" <<'PY'
import importlib.util
import json
import sys
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


probe_path = sys.argv[1]
spec = importlib.util.spec_from_file_location("probe_managed_bok", probe_path)
probe = importlib.util.module_from_spec(spec)
if spec.loader is None:
    raise SystemExit("could not load managed BoK probe")
spec.loader.exec_module(probe)


class SinkHandler(BaseHTTPRequestHandler):
    request_count = 0

    def _record(self):
        type(self).request_count += 1
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(b'{}')

    def do_GET(self):
        self._record()

    def do_POST(self):
        self._record()

    def log_message(self, format, *args):
        pass


class RedirectHandler(BaseHTTPRequestHandler):
    sink_url = ""

    def _redirect(self):
        payload = json.dumps({"redirect": True}).encode("utf-8")
        self.send_response(302)
        self.send_header("Location", self.sink_url)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def do_GET(self):
        self._redirect()

    def do_POST(self):
        self._redirect()

    def log_message(self, format, *args):
        pass


sink_server = ThreadingHTTPServer(("127.0.0.1", 0), SinkHandler)
redirect_server = ThreadingHTTPServer(("127.0.0.1", 0), RedirectHandler)
RedirectHandler.sink_url = f"http://127.0.0.1:{sink_server.server_port}/sink"
sink_thread = threading.Thread(target=sink_server.serve_forever, daemon=True)
redirect_thread = threading.Thread(target=redirect_server.serve_forever, daemon=True)
sink_thread.start()
redirect_thread.start()

try:
    redirect_url = f"http://127.0.0.1:{redirect_server.server_port}"
    get_status, _ = probe.get_json(redirect_url, "/redirect", 2.0)
    if get_status != 302:
        raise SystemExit(f"redirecting GET was not returned as HTTP 302: {get_status}")
    post_status, _ = probe.post_json(redirect_url, "/redirect", {}, 2.0)
    if post_status != 302:
        raise SystemExit(f"redirecting POST was not returned as HTTP 302: {post_status}")
    if SinkHandler.request_count != 0:
        raise SystemExit(f"redirect sink received {SinkHandler.request_count} request(s)")
finally:
    redirect_server.shutdown()
    sink_server.shutdown()
    redirect_server.server_close()
    sink_server.server_close()
PY

rg -F -- 'f8ada934ad295f618d51a0d769847a9ffdd4bfbf' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '1.0.56' "$SNAPSHOT_SCRIPT" >/dev/null
! rg -F -- '1.0.57' "$SNAPSHOT_SCRIPT" >/dev/null
! rg -F -- 'LITERATE_MODELING_' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'etc/runweb-wip.sh' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'git -C "$SOURCE_REPOSITORY" archive' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'cmp -s "$producer_root_lockfile" "$snapshot_root_lockfile"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'cmp -s "$producer_ui_lockfile" "$snapshot_ui_lockfile"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '! -d "$producer_node_modules"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '-L "$producer_node_modules"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '-L "$producer_ui_bundle"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '! -s "$producer_ui_bundle"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'DEFAULT_RUNTIME_EVIDENCE_ROOT="/Users/asami/src/dev2025/simplemodeling-org"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'RUNTIME_EVIDENCE_ROOT="${TEXTUS_SIMPLEMODELING_RUNTIME_EVIDENCE_ROOT:-$DEFAULT_RUNTIME_EVIDENCE_ROOT}"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'target/media/development-process/object-modeling/ja/final.mp4' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'target/media/development-process/object-modeling/en/final.mp4' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'target/media/development-process/knowledge-modeling/ja/final.mp4' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'target/media/development-process/knowledge-modeling/en/final.mp4' "$SNAPSHOT_SCRIPT" >/dev/null
[[ "$(rg -c -F -- 'final.mp4' "$SNAPSHOT_SCRIPT")" == "4" ]]
rg -F -- 'OBJECT_MODELING_JA_FINAL_MP4_SHA256="${TEXTUS_SIMPLEMODELING_OBJECT_MODELING_JA_FINAL_MP4_SHA256:-7af98dc34851b30e060e8c6e99b4f7e18e6ad61a7d3c9e8baf79e5e52f826135}"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'OBJECT_MODELING_EN_FINAL_MP4_SHA256="${TEXTUS_SIMPLEMODELING_OBJECT_MODELING_EN_FINAL_MP4_SHA256:-a880a4d4871c766e136d7a6edd5e7321826c119dbff4344fbc5d2f7226b71977}"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'KNOWLEDGE_MODELING_JA_FINAL_MP4_SHA256="${TEXTUS_SIMPLEMODELING_KNOWLEDGE_MODELING_JA_FINAL_MP4_SHA256:-123a2210df82ad98929c47059fce435b8f0e41961aafb43c66beb04f4af5afea}"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'KNOWLEDGE_MODELING_EN_FINAL_MP4_SHA256="${TEXTUS_SIMPLEMODELING_KNOWLEDGE_MODELING_EN_FINAL_MP4_SHA256:-8f71997365f46ca9a53fef8aaf94fa95292d70a4cb7b45971f00b6b9f59b1afc}"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '--runtime-evidence-root)' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '--object-modeling-ja-final-mp4-sha256)' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '--object-modeling-en-final-mp4-sha256)' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '--knowledge-modeling-ja-final-mp4-sha256)' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '--knowledge-modeling-en-final-mp4-sha256)' "$SNAPSHOT_SCRIPT" >/dev/null
! rg -F -- 'RUNTIME_EVIDENCE_RELATIVE_PATHS+=(' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '! -d "$RUNTIME_EVIDENCE_ROOT" || -L "$RUNTIME_EVIDENCE_ROOT"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'for index in "${!RUNTIME_EVIDENCE_RELATIVE_PATHS[@]}"; do' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '! -f "$runtime_evidence_source" || -L "$runtime_evidence_source" || ! -s "$runtime_evidence_source"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '! "$runtime_evidence_sha256" =~ ^[0-9a-f]{64}$' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'shasum -a 256 "$runtime_evidence_source"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'actual_runtime_evidence_sha256" != "$runtime_evidence_sha256"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'runtime_evidence_snapshot="$OUTPUT_DIRECTORY/${RUNTIME_EVIDENCE_RELATIVE_PATHS[$index]}"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '[[ -e "$runtime_evidence_snapshot" || -L "$runtime_evidence_snapshot" ]]' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'cp -- "$runtime_evidence_source" "$runtime_evidence_snapshot"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '! -f "$runtime_evidence_snapshot" || -L "$runtime_evidence_snapshot" || ! -s "$runtime_evidence_snapshot"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'cmp -s "$runtime_evidence_source" "$runtime_evidence_snapshot"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'DEFAULT_COZY_LAUNCHER="/Users/asami/src/dev2025/cozy/out.d/bin/launcher"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'DEFAULT_COZY_LAUNCHER_SHA256="2998aac2317fca4941467ca157abe93c8bcffc282ec7cb6e79af1ef108a3f885"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'DEFAULT_COZY_CLASSPATH="/Users/asami/src/dev2025/cozy/target/classpath.txt"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'DEFAULT_COZY_CLASSPATH_SHA256="c790d46829c0b6c683a44f48f31fd2f9ecbce2bfcbb85a32c92ade34583401f2"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'DEFAULT_ARCADIA_CLASSPATH="/Users/asami/src/dev2025/arcadia/target/classpath.txt"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'DEFAULT_ARCADIA_CLASSPATH_SHA256="71d017afca9c296ee6653fd7ebd7bd68c1cc51cb5128b8b3a5b035c3d3e6b8bc"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '--cozy-launcher)' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '--cozy-launcher-sha256)' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '--cozy-classpath)' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '--cozy-classpath-sha256)' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'TEXTUS_SIMPLEMODELING_COZY_LAUNCHER:-$DEFAULT_COZY_LAUNCHER' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'TEXTUS_SIMPLEMODELING_COZY_LAUNCHER_SHA256:-$DEFAULT_COZY_LAUNCHER_SHA256' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'TEXTUS_SIMPLEMODELING_COZY_CLASSPATH:-$DEFAULT_COZY_CLASSPATH' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'TEXTUS_SIMPLEMODELING_COZY_CLASSPATH_SHA256:-$DEFAULT_COZY_CLASSPATH_SHA256' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'TEXTUS_SIMPLEMODELING_ARCADIA_CLASSPATH:-$DEFAULT_ARCADIA_CLASSPATH' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'TEXTUS_SIMPLEMODELING_ARCADIA_CLASSPATH_SHA256:-$DEFAULT_ARCADIA_CLASSPATH_SHA256' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '! -f "$COZY_LAUNCHER"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '-L "$COZY_LAUNCHER"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '! -x "$COZY_LAUNCHER"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '! -f "$COZY_CLASSPATH"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '-L "$COZY_CLASSPATH"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '! -s "$COZY_CLASSPATH"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '! -f "$ARCADIA_CLASSPATH"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '-L "$ARCADIA_CLASSPATH"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '! -s "$ARCADIA_CLASSPATH"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '^[0-9a-f]{64}$' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'shasum -a 256 "$COZY_LAUNCHER"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'actual_cozy_launcher_sha256" != "$COZY_LAUNCHER_SHA256"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'shasum -a 256 "$COZY_CLASSPATH"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'actual_cozy_classpath_sha256" != "$COZY_CLASSPATH_SHA256"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'shasum -a 256 "$ARCADIA_CLASSPATH"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'actual_arcadia_classpath_sha256" != "$ARCADIA_CLASSPATH_SHA256"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '--arcadia-classpath)' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '--arcadia-classpath-sha256)' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'cozy_tool_bin="$OUTPUT_DIRECTORY/.cozy-tool-bin"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'cozy_shim="$cozy_tool_bin/cozy"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'mkdir -- "$cozy_tool_bin"' "$SNAPSHOT_SCRIPT" >/dev/null
shim_exec_line="printf 'exec %q --classpath-file %q --main-class cozy.Cozy -- \"\$@\"\\n' \"\$COZY_LAUNCHER\" \"\$COZY_CLASSPATH\""
[[ "$(rg -c -F -- "$shim_exec_line" "$SNAPSHOT_SCRIPT")" == "1" ]]
rg -F -- 'arcadia_website_root="$OUTPUT_DIRECTORY/src/main/website"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'arcadia_material_kit_marker="$arcadia_website_web_inf/webapp.conf"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '[[ ! -d "$arcadia_website_root" || -L "$arcadia_website_root" ]]' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '[[ -e "$arcadia_material_kit_marker" || -L "$arcadia_material_kit_marker" ]]' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- "'name: material-kit-pro'" "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'arcadia_shim="$cozy_tool_bin/arcadia"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '[[ "${1:-}" == "site" ]] ||' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- 'Unsupported Arcadia operation' "$SNAPSHOT_SCRIPT" >/dev/null
arcadia_exec_line="printf 'exec %q --classpath-file %q --main-class arcadia.standalone.Standalone -- site --library src/main/website,src/main/arcadialib \"\$@\"\\n' \"\$COZY_LAUNCHER\" \"\$ARCADIA_CLASSPATH\""
[[ "$(rg -c -F -- "$arcadia_exec_line" "$SNAPSHOT_SCRIPT")" == "1" ]]
[[ "$(rg -c -F -- '} > "$arcadia_shim"' "$SNAPSHOT_SCRIPT")" == "1" ]]
rg -F -- 'chmod 700 "$arcadia_shim"' "$SNAPSHOT_SCRIPT" >/dev/null
rg -F -- '[[ ! -f "$arcadia_shim" || -L "$arcadia_shim" || ! -x "$arcadia_shim" ]]' "$SNAPSHOT_SCRIPT" >/dev/null
! rg -F -- 'require_literate_media_artifacts' "$SNAPSHOT_SCRIPT" >/dev/null
! rg -F -- 'media build' "$SNAPSHOT_SCRIPT" >/dev/null
! rg -F -- 'media publish' "$SNAPSHOT_SCRIPT" >/dev/null
! rg -n -e '(^|[[:space:];|&])ln[[:space:]]+-s([[:space:]]|$)' "$SNAPSHOT_SCRIPT" >/dev/null
! rg -F -- 'readlink' "$SNAPSHOT_SCRIPT" >/dev/null
! rg -F -- '-ef' "$SNAPSHOT_SCRIPT" >/dev/null
[[ "$(rg -c -e '^[[:space:]]*cp([[:space:]]|$)' "$SNAPSHOT_SCRIPT")" == "2" ]]
rg -F -- 'cp -- "$producer_ui_bundle" "$snapshot_ui_bundle"' "$SNAPSHOT_SCRIPT" >/dev/null
[[ "$(rg -c -F -- 'PATH="$cozy_tool_bin:$PATH" NODE_PATH="$producer_node_modules" /bin/bash etc/runweb-wip.sh' "$SNAPSHOT_SCRIPT")" == "1" ]]
! rg -F -- 'cozy.local-wrapper' "$SNAPSHOT_SCRIPT" >/dev/null
! rg -F -- 'MAIN_CLASS=' "$SNAPSHOT_SCRIPT" >/dev/null
! rg -n -e '(^|[[:space:];|&])grep([[:space:]].*)?(mainClass|main\.class|MAIN_CLASS|build\.sbt)' "$SNAPSHOT_SCRIPT" >/dev/null
! rg -n -e '(^|[[:space:];|&])sed([[:space:]].*)?(mainClass|main\.class|MAIN_CLASS|build\.sbt)' "$SNAPSHOT_SCRIPT" >/dev/null
! rg -F -- 'build.sbt' "$SNAPSHOT_SCRIPT" >/dev/null
! rg -ni -e 'coursier|(^|[[:space:];|&])cs([[:space:]]|$)' "$SNAPSHOT_SCRIPT" >/dev/null
! rg -e '(^|[[:space:]])(npm|yarn|pnpm)[[:space:]]+(install|ci|add|update)([[:space:]]|$)' "$SNAPSHOT_SCRIPT" >/dev/null
! rg -e '(^|[[:space:];|&])((\./)?sbt)([[:space:]]|$)' "$SNAPSHOT_SCRIPT" >/dev/null
! rg -F -- '/rest/v1/bok/bok-retrieval/replace-knowledge-source' "$PROBE_SCRIPT" >/dev/null

echo "MANAGED_BOOTSTRAP_TOOLS_OK"
