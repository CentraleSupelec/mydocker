import {
  DecoratorNode,
  DOMConversionMap,
  DOMConversionOutput,
  DOMExportOutput,
  EditorConfig,
  NodeKey,
  SerializedLexicalNode,
  Spread,
} from 'lexical';

export type SerializedImageNode = Spread<
  {
    src: string;
    altText: string;
  },
  SerializedLexicalNode
>;


const convertImageElement = (domNode: Node): null | DOMConversionOutput => {
  const img = domNode as HTMLImageElement;
  const {alt: altText, src} = img;
  const node = $createImageNode(src, altText);
  return {node};
}

export class ImageNode extends DecoratorNode<HTMLElement> {
  __src: string;
  __altText: string;

  static getType(): string {
    return 'image';
  }

  getSrc(): string {
    return this.__src;
  }

  getAltText(): string {
    return this.__altText;
  }

  static clone(node: ImageNode): ImageNode {
    return new ImageNode(node.__src, node.__altText, node.__key);
  }

  constructor(src: string, altText: string, key?: NodeKey) {
    super(key);
    this.__src = src;
    this.__altText = altText;
  }

  exportDOM(): DOMExportOutput {
    const element = document.createElement('img');
    element.setAttribute('src', this.__src);
    if (this.__altText) {
      element.setAttribute('alt', this.__altText);
    }
    return { element };
  }

  static importDOM(): DOMConversionMap | null {
    return {
      img: (node: Node) => ({
        conversion: convertImageElement,
        priority: 0,
      }),
    };
  }

  static importJSON(serializedNode: SerializedImageNode): ImageNode {
    const { src, altText } = serializedNode;
    return new ImageNode(src, altText);
  }

  decorate(): HTMLElement {
    const img = document.createElement('img');
    img.src = this.__src;
    img.alt = this.__altText;
    img.style.maxWidth = '100%';
    img.style.height = 'auto';
    return img;
  }
  
  exportJSON(): SerializedImageNode {
    return {
      type: 'image',
      src: this.__src,
      altText: this.__altText,
      version: 1,
    };
  }

  createDOM(_config: EditorConfig): HTMLElement {
    const img = document.createElement('img');
    img.src = this.__src;
    img.alt = this.__altText;
    img.style.maxWidth = '100%';
    img.style.height = 'auto';
    return img;
  }

  updateDOM(): false {
    return false;
  }
}

export function $createImageNode(src: string, altText = ''): ImageNode {
  return new ImageNode(src, altText);
}

export function $isImageNode(node: unknown): node is ImageNode {
  return node instanceof ImageNode;
}
