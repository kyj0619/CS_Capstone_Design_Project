const express = require('express');
const multer = require('multer');
const fs = require('fs');
const path = require('path');

const app = express();
const upload = multer({ dest: 'uploads/' });

app.post('/api/capture', upload.single('image'), (req, res) => {
  const uploadedFile = req.file;
  const originalFileName = req.file.originalname;

  // 새로운 파일 이름을 생성합니다.
  const newFileName = generateNewFileName(originalFileName);

  // 파일을 이동하고 이름을 변경합니다.
  const sourcePath = uploadedFile.path;
  const destinationPath = path.join(uploadedFile.destination, newFileName);
  fs.renameSync(sourcePath, destinationPath);

  // req.file.filename을 변경합니다.
  uploadedFile.filename = newFileName;

  // 나머지 파일 정보를 사용하여 필요한 작업을 수행합니다.
  res.send('File uploaded successfully.');
});

app.listen(3001, () => {
  console.log('Server is running on port 3001');
});

// 새로운 파일 이름을 생성하는 함수
function generateNewFileName(originalFileName) {
  return originalFileName;
}
