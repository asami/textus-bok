(() => {
  const _decodeHtmlEntities = (value) => {
    return new DOMParser().parseFromString(value, "text/html").documentElement.textContent || "";
  };

  const _scalar = (value) => {
    if (value === null || value === undefined) return "";
    if (typeof value === "object" && Object.prototype.hasOwnProperty.call(value, "value")) return _scalar(value.value);
    return String(value);
  };

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
      const url = _safeHttpUrl(value && value.uri);
      if (url) {
        const link = document.createElement("a");
        link.href = url.href;
        link.target = "_blank";
        link.rel = "noopener noreferrer";
        link.textContent = url.href;
        detail.append(link);
      } else {
        detail.append(_text("code", value && value.uri));
      }
    } else {
      detail.append(_text("span", value));
    }
    list.append(detail);
  };

  const _showDetail = (detail, node) => {
    detail.replaceChildren(_text("h3", _scalar(node.label) || "Selected node"));
    const list = document.createElement("dl");
    _appendDetail(list, "Node", node.nodeId);
    _appendDetail(list, "Kind", node.kind);
    _appendDetail(list, "Category", node.category);
    _appendDetail(list, "Definitions", (node.terms || []).map(term => _scalar(term.definition || term.title)).join("; "));
    _appendDetail(list, "CBD handoff", (node.componentReferences || []).map(reference => `${_scalar(reference.kind)}:${_scalar(reference.name)}`).join(", "));
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
      return [_scalar(node.nodeId), { x: 130 + column * 235, y: 85 + row * 135 }];
    }));
    relationships.forEach((relationship) => {
      const source = positions.get(_scalar(relationship.subjectId));
      const target = positions.get(_scalar(relationship.objectId));
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
      const position = positions.get(_scalar(node.nodeId));
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
      const select = () => _showDetail(detail, node);
      group.addEventListener("click", select);
      group.addEventListener("keydown", (event) => {
        if (event.key === "Enter" || event.key === " ") { event.preventDefault(); select(); }
      });
      group.append(circle, label);
      svg.append(group);
    });
    container.append(svg);
  };

  const source = document.getElementById("bok-knowledge-map-data");
  const container = document.getElementById("bok-knowledge-map-graph");
  const detail = document.getElementById("bok-knowledge-map-detail");
  const summary = document.getElementById("bok-knowledge-map-summary");
  if (!source || !container || !detail || !summary) return;
  try {
    const model = JSON.parse(_decodeHtmlEntities(source.textContent || "{}"));
    _draw(container, detail, summary, model.body || model);
  } catch (_) {
    container.append(_text("p", "The interactive graph is unavailable. Use the complete tables below."));
  }
})();
