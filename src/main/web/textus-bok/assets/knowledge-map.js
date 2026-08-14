(() => {
  const _decodeHtmlEntities = (value) => {
    return new DOMParser().parseFromString(value, "text/html").documentElement.textContent || "";
  };

  const _scalar = (value) => {
    if (value === null || value === undefined) return "";
    if (typeof value === "object" && Object.prototype.hasOwnProperty.call(value, "value")) return _scalar(value.value);
    return String(value);
  };

  const _field = (value, camel, snake) => value && (value[camel] ?? value[snake]);

  const _text = (tag, value) => {
    const element = document.createElement(tag);
    element.textContent = _scalar(value);
    return element;
  };

  const _safeHttpUrl = (value) => {
    try {
      const url = new URL(_scalar(value));
      return url.protocol === "https:" || url.protocol === "http:" ? url : null;
    } catch (_) {
      return null;
    }
  };

  const _appendDetail = (list, name, value) => {
    list.append(_text("dt", name));
    const detail = document.createElement("dd");
    if (name === "Evidence") {
      const uri = _field(value, "uri", "uri");
      const url = _safeHttpUrl(uri);
      if (url) {
        const link = document.createElement("a");
        link.href = url.href;
        link.target = "_blank";
        link.rel = "noopener noreferrer";
        link.textContent = url.href;
        detail.append(link);
      } else {
        detail.append(_text("code", uri));
      }
    } else {
      detail.append(_text("span", value));
    }
    list.append(detail);
  };

  const _showDetail = (detail, node) => {
    detail.replaceChildren(_text("h3", _scalar(node.label) || "Selected node"));
    const list = document.createElement("dl");
    _appendDetail(list, "Node", _field(node, "nodeId", "node_id"));
    _appendDetail(list, "Kind", node.kind);
    _appendDetail(list, "Category", node.category);
    _appendDetail(list, "Definitions", (node.terms || []).map(term => _scalar(term.definition || term.title)).join("; "));
    _appendDetail(list, "CBD handoff", (_field(node, "componentReferences", "component_references") || []).map(reference => `${_scalar(reference.kind)}:${_scalar(reference.name)}`).join(", "));
    _appendDetail(list, "Evidence", node.evidence);
    detail.append(list);
    detail.focus();
  };

  const _draw = (container, detail, summary, model) => {
    const nodes = Array.isArray(model.nodes) ? model.nodes : [];
    const relationships = Array.isArray(model.relationships) ? model.relationships : [];
    summary.textContent = `Nodes: ${nodes.length} · Relationships: ${relationships.length} · Truncated: ${_scalar(model.truncated)}`;
    if (nodes.length === 0) {
      container.append(_text("p", "No matching map nodes."));
      return;
    }
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("viewBox", "0 0 960 560");
    svg.setAttribute("role", "img");
    svg.setAttribute("aria-label", "Knowledge Map graph");
    const positions = new Map(nodes.map((node, index) => {
      const column = index % 4;
      const row = Math.floor(index / 4);
      return [_scalar(_field(node, "nodeId", "node_id")), { x: 130 + column * 235, y: 85 + row * 135 }];
    }));
    relationships.forEach((relationship) => {
      const source = positions.get(_scalar(_field(relationship, "subjectId", "subject_id")));
      const target = positions.get(_scalar(_field(relationship, "objectId", "object_id")));
      if (!source || !target) return;
      const edge = document.createElementNS(svg.namespaceURI, "line");
      edge.setAttribute("class", "bok-map-edge");
      edge.setAttribute("x1", source.x);
      edge.setAttribute("y1", source.y);
      edge.setAttribute("x2", target.x);
      edge.setAttribute("y2", target.y);
      svg.append(edge);
    });
    nodes.forEach((node) => {
      const position = positions.get(_scalar(_field(node, "nodeId", "node_id")));
      const group = document.createElementNS(svg.namespaceURI, "g");
      group.setAttribute("class", "bok-map-node");
      group.setAttribute("role", "button");
      group.setAttribute("tabindex", "0");
      group.setAttribute("aria-label", `Show details for ${_scalar(node.label)}`);
      group.setAttribute("transform", `translate(${position.x} ${position.y})`);
      const circle = document.createElementNS(svg.namespaceURI, "circle");
      circle.setAttribute("r", "30");
      const label = document.createElementNS(svg.namespaceURI, "text");
      label.setAttribute("text-anchor", "middle");
      label.setAttribute("y", "48");
      label.textContent = _scalar(node.label).slice(0, 28);
      const kind = document.createElementNS(svg.namespaceURI, "text");
      kind.setAttribute("class", "bok-map-node-kind");
      kind.setAttribute("text-anchor", "middle");
      kind.setAttribute("y", "66");
      kind.textContent = _scalar(node.kind);
      const select = () => _showDetail(detail, node);
      group.addEventListener("click", select);
      group.addEventListener("keydown", (event) => {
        if (event.key === "Enter" || event.key === " ") { event.preventDefault(); select(); }
      });
      group.setAttribute("data-node-kind", _scalar(node.kind));
      group.setAttribute("aria-label", `Show ${_scalar(node.kind) || "node"} details for ${_scalar(node.label)}`);
      group.append(circle, label, kind);
      svg.append(group);
    });
    container.append(svg);
  };

  const source = document.getElementById("bok-knowledge-map-data");
  const container = document.getElementById("bok-knowledge-map-graph");
  const detail = document.getElementById("bok-knowledge-map-detail");
  const summary = document.getElementById("bok-knowledge-map-summary");
  if (!source || !container || !detail || !summary) return;
  const profile = document.getElementById("bok-map-profile");
  const projectid = document.getElementById("bok-map-project-id");
  const requeststate = document.getElementById("bok-map-request-state");
  if (profile && projectid && requeststate) {
    const query = new URLSearchParams(window.location.search);
    const hasprofile = query.has("profile");
    const hasprojectid = query.has("projectId");
    const requestedprofile = query.get("profile");
    const requestedprojectid = query.get("projectId");
    const knownprofiles = ["official", "development", "project"];
    let invalidprofile = hasprofile && !knownprofiles.includes(requestedprofile);
    let conflictingselection = hasprojectid && requestedprofile !== "project";
    let initialinvalidorconflicting = invalidprofile || conflictingselection;
    const initialprofilelabel = hasprofile
      ? requestedprofile === "" ? "(empty)" : requestedprofile
      : "official (default)";
    const initialprojectidlabel = hasprojectid
      ? requestedprojectid === "" ? "(empty)" : requestedprojectid
      : "(omitted)";
    const _updateRequestState = () => {
      const selectedprofile = profile.value;
      const selectedprojectid = projectid.value.trim();
      projectid.disabled = selectedprofile !== "project";
      projectid.required = selectedprofile === "project";
      if (invalidprofile) {
        requeststate.textContent = `Request state: profile=${initialprofilelabel} (invalid selection; the operation will report a structured failure).`;
        if (conflictingselection) {
          requeststate.textContent += ` projectId=${initialprojectidlabel} also conflicts with this profile; the operation will report a structured failure.`;
        }
      } else if (conflictingselection) {
        requeststate.textContent = `Request state: profile=${initialprofilelabel}; projectId=${initialprojectidlabel} conflicts with this profile; the operation will report a structured failure.`;
      } else if (selectedprofile === "project") {
        requeststate.textContent = `Request state: profile=project; projectId=${selectedprojectid || "(required)"}`;
      } else {
        requeststate.textContent = `Request state: profile=${selectedprofile}; projectId is omitted unless project is selected.`;
      }
    };
    if (invalidprofile) {
      profile.value = "";
      profile.selectedIndex = -1;
    } else {
      profile.value = hasprofile ? requestedprofile : "official";
    }
    projectid.value = hasprojectid ? requestedprojectid : "";
    _updateRequestState();
    profile.addEventListener("change", () => {
      invalidprofile = false;
      conflictingselection = false;
      initialinvalidorconflicting = false;
      if (profile.value !== "project") projectid.value = "";
      _updateRequestState();
    });
    projectid.addEventListener("input", () => {
      _updateRequestState();
    });
    const form = profile.form;
    if (form) {
      form.addEventListener("submit", (event) => {
        if (initialinvalidorconflicting) event.preventDefault();
      });
    }
  }
  try {
    const model = JSON.parse(_decodeHtmlEntities(source.textContent || "{}"));
    _draw(container, detail, summary, model.body || model);
  } catch (_) {
    container.append(_text("p", "The interactive graph is unavailable. Use the complete tables below."));
  }
})();
