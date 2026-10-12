import type { Post } from './prototype';
export function postImages(post: Post): string[] {
  if (post.type !== 'article') return [];
  if (post.images?.length) return post.images;
  if (post.media) return [post.media];
  const name =
    post.topic === '语言成长'
      ? 'language'
      : post.topic === '创作与阅读'
        ? 'reading'
        : post.topic === '考试备考'
          ? 'study'
          : 'routine';
  return [`/media/note-${name}.svg`];
}
