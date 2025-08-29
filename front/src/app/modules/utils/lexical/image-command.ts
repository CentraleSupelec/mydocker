import { createCommand } from 'lexical';

export const INSERT_IMAGE_COMMAND = createCommand<{ src: string; altText?: string }>();
