import { Component, forwardRef, OnInit, ElementRef, ViewChild, ViewEncapsulation, Input, OnDestroy } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import {
  createEditor,
  $getRoot,
  $createParagraphNode,
  EditorState,
  FORMAT_TEXT_COMMAND,
  COMMAND_PRIORITY_EDITOR,
  LexicalEditor,
  $getSelection,
  $isRangeSelection,
  LexicalNode,
  $isNodeSelection,
  COMMAND_PRIORITY_LOW,
  KEY_TAB_COMMAND,
  TabNode,
  INSERT_TAB_COMMAND,
  INDENT_CONTENT_COMMAND,
  OUTDENT_CONTENT_COMMAND,
  LexicalCommand,
  COMMAND_PRIORITY_CRITICAL,
  $createTextNode
} from 'lexical';
import { $createHeadingNode, $createQuoteNode, $isHeadingNode, HeadingNode, HeadingTagType, QuoteNode, registerRichText } from '@lexical/rich-text';
import { createEmptyHistoryState, registerHistory } from '@lexical/history';

import { $generateHtmlFromNodes, $generateNodesFromDOM } from '@lexical/html';
import { LinkNode, $isLinkNode, TOGGLE_LINK_COMMAND, $toggleLink } from '@lexical/link';
import { $setBlocksType } from '@lexical/selection';
import { $isListNode, INSERT_CHECK_LIST_COMMAND, INSERT_ORDERED_LIST_COMMAND, INSERT_UNORDERED_LIST_COMMAND, ListItemNode, ListNode, registerCheckList, registerList } from '@lexical/list';
import { $getNearestBlockElementAncestorOrThrow, $getNearestNodeOfType, mergeRegister } from '@lexical/utils';
import { $createCodeNode, $isCodeNode, CodeNode } from '@lexical/code';
import { MatDialog } from '@angular/material/dialog';
import { LexicalLinkPopupComponent } from '../lexical-link-popup/lexical-link-popup.component';
import { $convertFromMarkdownString, $convertToMarkdownString } from '@lexical/markdown';
import { $createImageNode, ImageNode } from '../image-node';
import { INSERT_IMAGE_COMMAND } from '../image-command';
import { ImageDialogData, LexicalImagePopupComponent } from '../lexical-image-popup/lexical-image-popup.component';
import { blockTypeToBlockName, findTopLevelElement, getSelectedNode, indentOverTab, sanitizeUrl, theme, TRANSFORMERS } from 'src/app/modules/utils/lexical/lexical-utils';

const MAX_INDENT = 5

const INITIAL_TOOLBAR_STATE = {
  blockType: 'paragraph' as keyof typeof blockTypeToBlockName,
  isBold: false,
  isCode: false,
  isHighlight: false,
  isItalic: false,
  isLink: false,
  isStrikethrough: false,
  isUnderline: false,
  isMarkdown: false
};

@Component({
  selector: 'app-lexical-editor',
  templateUrl: './lexical-editor.component.html',
  styleUrls: ['./lexical-editor.component.css'],
  encapsulation: ViewEncapsulation.None,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => LexicalEditorComponent),
      multi: true
    }
  ]
})

export class LexicalEditorComponent implements OnInit, OnDestroy, ControlValueAccessor {
  constructor(public dialog: MatDialog) { }

  private unregister: () => void = () => {};

  ngOnDestroy(): void {
    this.unregister();
  }

  @Input() isValid?: boolean = true;
  @ViewChild('editorContainer', { static: true }) editorContainer!: ElementRef<HTMLDivElement>;

  editor: LexicalEditor = createEditor();
  headingLevels: HeadingTagType[] = ['h1', 'h2', 'h3', 'h4', 'h5', 'h6'];

  private onChange: (value: string) => void = () => { };
  private onTouched: () => void = () => { };
  public toolbarState = INITIAL_TOOLBAR_STATE

  linkEditorVisible = false;
  linkUrl = '';
  popupPosition = { top: 0, left: 0 };
  isTouched = false;
  initialUpdateDone = false;

  handleHeadingNode = (selectedElement: LexicalNode) => {
    const type = $isHeadingNode(selectedElement)
      ? selectedElement.getTag()
      : selectedElement.getType();

    if (type in blockTypeToBlockName) {
      this.toolbarState.blockType = type as keyof typeof blockTypeToBlockName;
    }
  }

  ngOnInit(): void {
    const initialConfig = {
      namespace: 'MyDocker',
      nodes: [HeadingNode, QuoteNode, LinkNode, CodeNode, ListNode, ListItemNode, TabNode, ImageNode],
      onError: (error: Error) => {
        console.error(error);
      },
      theme: theme
    };

    this.editor = createEditor(initialConfig);

    const container = this.editorContainer.nativeElement;
    this.editor.setRootElement(container);
    container.contentEditable = 'true';

    this.unregister = mergeRegister(
      registerRichText(this.editor),
      registerHistory(this.editor, createEmptyHistoryState(), 300),
      registerList(this.editor),
      registerCheckList(this.editor),
      this.editor.registerCommand(
        INSERT_IMAGE_COMMAND,
        ({ src, altText }) => {
          this.editor.update(() => {
            const selection = $getSelection();
            if ($isRangeSelection(selection)) {
              const imageNode = $createImageNode(src, altText || '');
              selection.insertNodes([imageNode]);
            }
          });
          return true;
        },
        COMMAND_PRIORITY_EDITOR
      ),
      this.editor.registerCommand(
        TOGGLE_LINK_COMMAND,
        (payload) => {
          if (payload === null) {
            $toggleLink(payload);
            return true;
          } else if (typeof payload === 'string') {
            $toggleLink(payload);
            return true;
          } else {
            return false;
          }
        },
        COMMAND_PRIORITY_LOW,
      ),
      this.editor.registerCommand(
        KEY_TAB_COMMAND,
        (event) => {
          const selection = $getSelection();
          if (!$isRangeSelection(selection)) {
            return false;
          }
          event.preventDefault();
          const command: LexicalCommand<void> = indentOverTab(selection)
            ? event.shiftKey
              ? OUTDENT_CONTENT_COMMAND
              : INDENT_CONTENT_COMMAND
            : INSERT_TAB_COMMAND;
          return this.editor.dispatchCommand(command, undefined);
        },
        COMMAND_PRIORITY_EDITOR,
      ),

      this.editor.registerCommand(
        INDENT_CONTENT_COMMAND,
        () => {
          if (MAX_INDENT == null) {
            return false;
          }

          const selection = $getSelection();
          if (!$isRangeSelection(selection)) {
            return false;
          }

          const indents = selection
            .getNodes()
            .map((node) =>
              $getNearestBlockElementAncestorOrThrow(node).getIndent(),
            );

          return Math.max(...indents) + 1 >= MAX_INDENT;
        },
        COMMAND_PRIORITY_CRITICAL,
      ),
      this.editor.registerUpdateListener(this.updateListener)
    )
  }

  updateListener = ({ editorState }: { editorState: EditorState }) => {
    editorState.read(() => {
      const root = $getRoot();
      const firstChild = root.getFirstChild();
      if ($isCodeNode(firstChild) && firstChild.getLanguage() === 'markdown') {
        this.toolbarState.isMarkdown = true;
      } else {
        this.toolbarState.isMarkdown = false;
      }

      const selection = $getSelection();
      if ($isRangeSelection(selection)) {
        const anchorNode = selection.anchor.getNode();
        const element = findTopLevelElement(anchorNode);
        const elementKey = element.getKey();
        const elementDOM = this.editor.getElementByKey(elementKey);

        const node = getSelectedNode(selection);
        const parent = node.getParent();
        const isParentLinkNode = $isLinkNode(parent);
        const isLinkNode = $isLinkNode(node);
        const isLink = isParentLinkNode || isLinkNode;

        if (isLinkNode) {
          this.linkUrl = (node as LinkNode).getURL()
        } else if (isParentLinkNode) {
          this.linkUrl = (node.getParent() as LinkNode).getURL()
        } else {
          this.linkUrl = 'https://'
        }

        this.toolbarState.isLink = isLink

        if (elementDOM !== null) {
          if ($isListNode(element)) {
            const parentList = $getNearestNodeOfType<ListNode>(
              anchorNode,
              ListNode,
            );
            const type = parentList
              ? parentList.getListType()
              : element.getListType();
            this.toolbarState.blockType = type
          } else {
            this.handleHeadingNode(element);
          }
        }
      }

      if ($isRangeSelection(selection)) {
        this.toolbarState.isBold = selection.hasFormat('bold');
        this.toolbarState.isItalic = selection.hasFormat('italic');
        this.toolbarState.isUnderline = selection.hasFormat('underline');
        this.toolbarState.isStrikethrough = selection.hasFormat('strikethrough');
        this.toolbarState.isHighlight = selection.hasFormat('highlight');
        this.toolbarState.isCode = selection.hasFormat('code');
      }
      if ($isNodeSelection(selection)) {
        const nodes = selection.getNodes();
        for (const selectedNode of nodes) {
          const parentList = $getNearestNodeOfType<ListNode>(
            selectedNode,
            ListNode,
          );
          if (parentList) {
            const type = parentList.getListType();
            this.toolbarState.blockType = type;
          } else {
            const selectedElement = findTopLevelElement(selectedNode);
            this.handleHeadingNode(selectedElement);
          }
        }
      }
      const html = $generateHtmlFromNodes(this.editor, null);
      this.onChange(html);


      if (this.initialUpdateDone && !this.isTouched) {
        this.isTouched = true;
      }

      if (!this.initialUpdateDone) {
        this.initialUpdateDone = true;
      }
    });
  }

  setTouchedState(isTouched: boolean) {
    this.isTouched = isTouched;
  }

  writeValue(value: string): void {
    if (!this.editor || value === undefined || value === null) return;

    this.editor.update(() => {
      const root = $getRoot();
      root.clear();

      if (value.trim() === '') {
        const paragraph = $createParagraphNode();
        root.append(paragraph);
        return;
      }

      const parser = new DOMParser();
      const doc = parser.parseFromString(value, 'text/html');
      const nodes = $generateNodesFromDOM(this.editor, doc);

      root.append(...nodes);
    });
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  format(command: 'bold' | 'italic' | 'underline' | 'code' | 'strikethrough') {
    this.editor.dispatchCommand(FORMAT_TEXT_COMMAND, command);
  }

  formatQuote() {
    this.editor.update(
      () => {
        const selection = $getSelection();
        $setBlocksType(selection, () => $createQuoteNode());
      }
    )
  };

  formatHeading = (headingSize: HeadingTagType) => {
    this.editor.update(() => {
      const selection = $getSelection();
      $setBlocksType(selection, () => $createHeadingNode(headingSize));
    });
  }

  formatBulletList = () => {
    if (this.toolbarState.blockType !== 'bullet') {
      this.editor.dispatchCommand(INSERT_UNORDERED_LIST_COMMAND, undefined);
    } else {
      this.formatParagraph();
    }
  };

  formatCheckList = () => {
    if (this.toolbarState.blockType !== 'check') {
      this.editor.dispatchCommand(INSERT_CHECK_LIST_COMMAND, undefined);
    } else {
      this.formatParagraph();
    }
  };

  formatNumberedList = () => {
    if (this.toolbarState.blockType !== 'number') {
      this.editor.dispatchCommand(INSERT_ORDERED_LIST_COMMAND, undefined);
    } else {
      this.formatParagraph();
    }
  };

  formatParagraph = () => {
    this.editor.update(() => {
      const selection = $getSelection();
      $setBlocksType(selection, () => $createParagraphNode());
    });
  };

  formatCode = () => {
    if (this.toolbarState.blockType !== 'code') {
      this.editor.update(() => {
        let selection = $getSelection();
        if (!selection) {
          return;
        }
        if (!$isRangeSelection(selection) || selection.isCollapsed()) {
          $setBlocksType(selection, () => $createCodeNode());
        } else {
          const textContent = selection.getTextContent();
          const codeNode = $createCodeNode();
          selection.insertNodes([codeNode]);
          selection = $getSelection();
          if ($isRangeSelection(selection)) {
            selection.insertRawText(textContent);
          }
        }
      });
    }
  };

  openImageDialog(): void {
    const dialogRef = this.dialog.open(LexicalImagePopupComponent, {
      data: { link: 'https://', alt: '' }
    });

    dialogRef.afterClosed().subscribe((result: ImageDialogData) => {
      if (result.link) {
        this.linkUrl = result.link;
        this.editor.dispatchCommand(INSERT_IMAGE_COMMAND, { src: result.link, altText: result.alt });
      }
    });
  }

  openLinkDialog(): void {
    const dialogRef = this.dialog.open(LexicalLinkPopupComponent, {
      data: this.linkUrl
    });

    dialogRef.afterClosed().subscribe(result => {
      this.linkUrl = result;
      this.editor.dispatchCommand(
        TOGGLE_LINK_COMMAND,
        result === undefined ? null : sanitizeUrl(result),
      );
    });
  }

  onEditorKeydown(event: KeyboardEvent) {
    if (event.key === 'Tab') {
      event.preventDefault(); // stop Angular/browser from moving focus
    }
  }

  handleMarkdownToggle = () => {
    this.editor.update(() => {
      const root = $getRoot();
      const firstChild = root.getFirstChild();
      if ($isCodeNode(firstChild) && firstChild.getLanguage() === 'markdown') {
        $convertFromMarkdownString(
          firstChild.getTextContent(),
          TRANSFORMERS,
          undefined,
          true,
        );
      } else {
        const markdown = $convertToMarkdownString(
          TRANSFORMERS,
          undefined,
          true,
        );
        const codeNode = $createCodeNode('markdown');
        codeNode.append($createTextNode(markdown));
        root.clear().append(codeNode);
        if (markdown.length === 0) {
          codeNode.select();
        }
      }
    });
  }
}
