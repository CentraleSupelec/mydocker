import { formatUrl } from "@lexical/link";
import { $isAtNodeEnd } from "@lexical/selection";
import { $filter, $findMatchingParent, $getNearestBlockElementAncestorOrThrow } from "@lexical/utils";
import { $createRangeSelection, $isBlockElementNode, $isRootOrShadowRoot, $normalizeSelection__EXPERIMENTAL, ElementNode, RangeSelection, TextNode } from "lexical";
import { LexicalNode } from "lexical/LexicalNode";
import { IMAGE } from "./image-transformer";
import { CHECK_LIST, ELEMENT_TRANSFORMERS, MULTILINE_ELEMENT_TRANSFORMERS, TEXT_FORMAT_TRANSFORMERS, TEXT_MATCH_TRANSFORMERS, Transformer } from "@lexical/markdown";

export const SUPPORTED_URL_PROTOCOLS = new Set([
  'http:',
  'https:',
  'mailto:',
  'sms:',
  'tel:',
]);

export const theme = {
  code: 'editor-code',
  heading: {
    h1: 'editor-heading-h1',
    h2: 'editor-heading-h2',
    h3: 'editor-heading-h3',
    h4: 'editor-heading-h4',
    h5: 'editor-heading-h5',
  },
  image: 'editor-image',
  link: 'editor-link',
  list: {
    listitem: 'editor-listitem',
    nested: {
      listitem: 'editor-nested-listitem',
    },
    ol: 'editor-list-ol',
    ul: 'editor-list-ul',
  },
  quote: 'quote-node',
  text: {
    bold: 'lexical-bold',
    italic: 'lexical-italic',
    underline: 'lexical-underline',
    strikethrough: 'lexical-strike',
    code: 'lexical-code',
  },
}

// Source: https://github.com/facebook/lexical/blob/25d77f6afdc3e777a6933a350eeafa530c8fc9a5/packages/lexical-playground/src/context/ToolbarContext.tsx#L32
// Copied to avoid installing react
export const blockTypeToBlockName = {
  bullet: 'Bulleted List',
  check: 'Check List',
  code: 'Code Block',
  h1: 'Heading 1',
  h2: 'Heading 2',
  h3: 'Heading 3',
  h4: 'Heading 4',
  h5: 'Heading 5',
  h6: 'Heading 6',
  number: 'Numbered List',
  paragraph: 'Normal',
  quote: 'Quote',
};

export const TRANSFORMERS: Array<Transformer> = [
  IMAGE,
  CHECK_LIST,
  ...ELEMENT_TRANSFORMERS,
  ...MULTILINE_ELEMENT_TRANSFORMERS,
  ...TEXT_FORMAT_TRANSFORMERS,
  ...TEXT_MATCH_TRANSFORMERS,
];

// Source: https://github.com/facebook/lexical/blob/25d77f6afdc3e777a6933a350eeafa530c8fc9a5/packages/lexical-playground/src/plugins/ToolbarPlugin/index.tsx#L537
// Copied to avoid installing react
export function findTopLevelElement(node: LexicalNode) {
  let topLevelElement =
    node.getKey() === 'root'
      ? node
      : $findMatchingParent(node, (e) => {
        const parent = e.getParent();
        return parent !== null && $isRootOrShadowRoot(parent);
      });

  if (topLevelElement === null) {
    topLevelElement = node.getTopLevelElementOrThrow();
  }
  return topLevelElement;
}

// Source: https://github.com/facebook/lexical/blob/25d77f6afdc3e777a6933a350eeafa530c8fc9a5/packages/lexical-playground/src/utils/getSelectedNode.ts#L11
// Copied to avoid installing react
export function getSelectedNode(selection: RangeSelection): TextNode | ElementNode {
  const anchor = selection.anchor;
  const focus = selection.focus;
  const anchorNode = selection.anchor.getNode();
  const focusNode = selection.focus.getNode();
  if (anchorNode === focusNode) {
    return anchorNode;
  }
  const isBackward = selection.isBackward();
  if (isBackward) {
    return $isAtNodeEnd(focus) ? anchorNode : focusNode;
  } else {
    return $isAtNodeEnd(anchor) ? anchorNode : focusNode;
  }
}

// Source: https://github.com/facebook/lexical/blob/25d77f6afdc3e777a6933a350eeafa530c8fc9a5/packages/lexical-react/src/LexicalTabIndentationPlugin.tsx#L32
// Copied to avoid installing react
export function indentOverTab(selection: RangeSelection): boolean {
  const nodes = selection.getNodes();
  const canIndentBlockNodes = $filter(nodes, (node) => {
    if ($isBlockElementNode(node) && node.canIndent()) {
      return node;
    }
    return null;
  });
  // 1. If selection spans across canIndent block nodes: indent
  if (canIndentBlockNodes.length > 0) {
    return true;
  }
  // 2. If first (anchor/focus) is at block start: indent
  const anchor = selection.anchor;
  const focus = selection.focus;
  const first = focus.isBefore(anchor) ? focus : anchor;
  const firstNode = first.getNode();
  const firstBlock = $getNearestBlockElementAncestorOrThrow(firstNode);
  if (firstBlock.canIndent()) {
    const firstBlockKey = firstBlock.getKey();
    let selectionAtStart = $createRangeSelection();
    selectionAtStart.anchor.set(firstBlockKey, 0, 'element');
    selectionAtStart.focus.set(firstBlockKey, 0, 'element');
    selectionAtStart = $normalizeSelection__EXPERIMENTAL(selectionAtStart);
    if (selectionAtStart.anchor.is(first)) {
      return true;
    }
  }
  // 3. Else: tab
  return false;
}

// Source: https://github.com/facebook/lexical/blob/25d77f6afdc3e777a6933a350eeafa530c8fc9a5/packages/lexical-link/src/index.ts#L166
// Copied because the function is not exported
export function sanitizeUrl(url: string): string {
  url = formatUrl(url);
  try {
    const parsedUrl = new URL(formatUrl(url));
    if (!SUPPORTED_URL_PROTOCOLS.has(parsedUrl.protocol)) {
      return 'about:blank';
    }
  } catch {
    return url;
  }
  return url;
}
