<template>
  <el-dialog
    title="批量导入音色"
    :visible.sync="dialogVisible"
    width="600px"
    @close="handleClose"
    :append-to-body="true"
  >
    <el-form :model="importForm" ref="importForm" label-width="100px">
      <el-form-item label="音色数据" prop="voiceData">
        <el-input
          type="textarea"
          v-model="importForm.voiceData"
          :rows="10"
          placeholder="请输入音色数据，每行一个音色，格式：音色编码,音色名称,语言类型,排序号,备注"
        ></el-input>
      </el-form-item>
      <el-form-item label="音频示例" prop="voiceDemo">
        <el-upload
          class="upload-demo"
          drag
          action="/api/upload"
          :on-success="handleUploadSuccess"
          :before-upload="beforeUpload"
          multiple
        >
          <i class="el-icon-upload"></i>
          <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
          <div class="el-upload__tip" slot="tip">支持mp3、wav格式音频文件</div>
        </el-upload>
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">取 消</el-button>
      <el-button type="primary" @click="handleImport">导 入</el-button>
    </div>
  </el-dialog>
</template>

<script>
import Api from '@/apis/api';

export default {
  name: 'BatchImportVoiceDialog',
  props: {
    showDialog: Boolean,
    ttsModelId: String
  },
  data() {
    return {
      dialogVisible: this.showDialog,
      importForm: {
        voiceData: '',
        voiceDemo: ''
      },
      uploadedFiles: []
    };
  },
  watch: {
    showDialog(val) {
      this.dialogVisible = val;
      if (!val) {
        this.resetForm();
      }
    }
  },
  methods: {
    handleClose() {
      this.dialogVisible = false;
      this.$emit('update:showDialog', false);
      this.resetForm();
    },
    resetForm() {
      this.importForm = {
        voiceData: '',
        voiceDemo: ''
      };
      this.uploadedFiles = [];
    },
    beforeUpload(file) {
      const isAudio = file.type === 'audio/mp3' || file.type === 'audio/wav';
      if (!isAudio) {
        this.$message.error('只能上传音频文件！');
        return false;
      }
      return true;
    },
    handleUploadSuccess(response, file) {
      this.uploadedFiles.push({
        name: file.name,
        url: response.data.url
      });
      this.$message.success('文件上传成功');
    },
    handleImport() {
      if (!this.importForm.voiceData.trim()) {
        this.$message.warning('请输入音色数据');
        return;
      }

      const lines = this.importForm.voiceData.split('\n').filter(line => line.trim());
      const voices = lines.map(line => {
        const [voiceCode, voiceName, languageType, sort, remark] = line.split(',').map(item => item.trim());
        const voiceData = {
          ttsModelId: this.ttsModelId,
          voiceCode,
          voiceName,
          languageType: languageType || '中文',
          sort: parseInt(sort) || 1,
          remark: remark || ''
        };
        
        // 如果找到对应的音频文件，则添加voiceDemo字段
        const demoFile = this.uploadedFiles.find(f => f.name.includes(voiceCode));
        if (demoFile?.url) {
          voiceData.voiceDemo = demoFile.url;
        }
        
        return voiceData;
      });

      // 批量保存音色
      const savePromises = voices.map(voice => 
        new Promise((resolve, reject) => {
          Api.timbre.saveVoice(voice, (response) => {
            if (response.code === 0) {
              resolve();
            } else {
              reject(new Error(response.msg || '保存失败'));
            }
          });
        })
      );

      Promise.all(savePromises)
        .then(() => {
          this.$message.success('批量导入成功');
          this.$emit('import-success');
          this.handleClose();
        })
        .catch(error => {
          this.$message.error(`导入失败: ${error.message}`);
        });
    }
  }
};
</script>

<style scoped>
.upload-demo {
  width: 100%;
}
.el-upload {
  width: 100%;
}
.el-upload-dragger {
  width: 100%;
}
</style> 