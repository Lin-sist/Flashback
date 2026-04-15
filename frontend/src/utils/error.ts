export const toUserMessage = (error: unknown) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return 'Request failed'
}
