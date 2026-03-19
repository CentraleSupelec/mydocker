
export interface IContent {
  id: number;
  title: string;
  richText: string;
  slug: string;
  enabled: boolean;
}

export interface IContentUpdateDto {
  title: string;
  richText: string;
  enabled: boolean;
}
