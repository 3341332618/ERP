import { ElMessage } from 'element-plus'

const maxImageSize = 200 * 1024

export function validDocumentImageFile(file: File) {
  const validType = ['image/jpeg', 'image/png'].includes(file.type)
  const validName = /\.(jpe?g|png)$/i.test(file.name)
  if (!validType && !validName) {
    ElMessage.error('请上传JPG/PNG类型格式文件')
    return false
  }
  if (file.size > maxImageSize) {
    ElMessage.error('上传文件大小不能超过200KB')
    return false
  }
  return true
}

export function readImageAsDataUrl(file: File) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })
}

export async function readValidDocumentImage(file?: File) {
  if (!file || !validDocumentImageFile(file)) {
    return null
  }
  return readImageAsDataUrl(file)
}
