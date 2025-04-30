<template>
  <div class="welcome">
    <HeaderBar />

    <div class="operation-bar">
      <h2 class="page-title">固件管理</h2>
      <div class="right-operations">
        <el-input placeholder="请输入固件名称或版本号查询" v-model="searchKeyword"
                 class="search-input" @keyup.enter.native="handleSearch" clearable />
        <el-button class="btn-search" @click="handleSearch">搜索</el-button>
        <el-button type="primary" @click="handleAddOta">新增固件</el-button>
      </div>
    </div>

    <div class="main-wrapper">
      <div class="content-panel">
        <div class="content-area">
          <el-card class="ota-card" shadow="never">
            <el-table
              ref="otaTable"
              :data="paginatedOtaList"
              @selection-change="handleSelectionChange"
              class="transparent-table"
              v-loading="loading"
              :header-cell-class-name="headerCellClassName">
              <el-table-column type="selection" align="center" width="80"></el-table-column>
              <el-table-column label="固件名称" prop="firmwareName" align="center"></el-table-column>
              <el-table-column label="版本号" prop="version" align="center"></el-table-column>
              <el-table-column label="类型" prop="type" align="center"></el-table-column>
              <el-table-column label="大小" align="center">
                <template slot-scope="scope">
                  {{ formatFileSize(scope.row.size) }}
                </template>
              </el-table-column>
              <el-table-column label="下载地址" prop="firmwarePath" align="center" show-overflow-tooltip></el-table-column>
              <el-table-column label="备注" prop="remark" align="center" show-overflow-tooltip></el-table-column>
              <el-table-column label="创建时间" align="center">
                <template slot-scope="scope">
                  {{ formatDate(scope.row.createDate) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" align="center" width="200">
                <template slot-scope="scope">
                  <el-button
                    size="mini"
                    type="primary"
                    @click="handleEdit(scope.row)">
                    编辑
                  </el-button>
                  <el-button
                    size="mini"
                    type="danger"
                    @click="handleDelete(scope.row)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="table_bottom">
              <div class="ctrl_btn">
                <el-button size="mini" type="primary" class="select-all-btn" @click="toggleAllSelection">
                  {{ isAllSelected ? '取消全选' : '全选' }}
                </el-button>
                <el-button size="mini" type="danger" icon="el-icon-delete"
                  @click="deleteSelected">批量删除</el-button>
              </div>
              <div class="custom-pagination">
                <el-select v-model="pageSize" @change="handlePageSizeChange" class="page-size-select">
                  <el-option
                    v-for="item in pageSizeOptions"
                    :key="item"
                    :label="`${item}条/页`"
                    :value="item">
                  </el-option>
                </el-select>
                <button class="pagination-btn" :disabled="currentPage === 1" @click="goFirst">首页</button>
                <button class="pagination-btn" :disabled="currentPage === 1" @click="goPrev">上一页</button>
                <button
                  v-for="page in visiblePages"
                  :key="page"
                  class="pagination-btn"
                  :class="{ active: page === currentPage }"
                  @click="goToPage(page)">
                  {{ page }}
                </button>
                <button class="pagination-btn" :disabled="currentPage === pageCount" @click="goNext">下一页</button>
                <span class="total-text">共{{ total }}条记录</span>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px">
      <el-form :model="otaForm" :rules="otaRules" ref="otaForm" label-width="100px">
        <el-form-item label="固件名称" prop="firmwareName">
          <el-input v-model="otaForm.firmwareName" placeholder="请输入固件名称"></el-input>
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="otaForm.version" placeholder="请输入版本号，例如：v1.0.0"></el-input>
        </el-form-item>
        <el-form-item label="固件类型" prop="type">
          <el-input v-model="otaForm.type" placeholder="请输入固件类型，例如：ESP32"></el-input>
        </el-form-item>
        <el-form-item label="大小(字节)" prop="size">
          <el-input v-model.number="otaForm.size" placeholder="请输入固件大小（字节）" type="number"></el-input>
        </el-form-item>
        <el-form-item label="下载地址" prop="firmwarePath">
          <el-input v-model="otaForm.firmwarePath" placeholder="请输入固件下载URL"></el-input>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="otaForm.sort" :min="0" :max="999"></el-input-number>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="otaForm.remark" type="textarea" :rows="3" placeholder="请输入备注说明"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveOta" :loading="saveLoading">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Api from '@/apis/api';
import HeaderBar from "@/components/HeaderBar.vue";

export default {
  components: { HeaderBar },
  data() {
    return {
      otaList: [],
      selectedOtas: [],
      isAllSelected: false,
      searchKeyword: '',
      activeSearchKeyword: '',
      currentPage: 1,
      pageSize: 10,
      pageSizeOptions: [10, 20, 50, 100],
      total: 0,
      dialogVisible: false,
      dialogTitle: '新增固件',
      saveLoading: false,
      loading: false,
      otaForm: {
        id: '',
        firmwareName: '',
        version: '',
        type: '',
        size: null,
        firmwarePath: '',
        remark: '',
        sort: 0
      },
      otaRules: {
        firmwareName: [
          { required: true, message: '请输入固件名称', trigger: 'blur' }
        ],
        version: [
          { required: true, message: '请输入版本号', trigger: 'blur' }
        ],
        firmwarePath: [
          { required: true, message: '请输入下载地址', trigger: 'blur' },
          { type: 'url', message: '请输入正确的URL格式', trigger: 'blur' }
        ]
      }
    };
  },
  computed: {
    filteredOtaList() {
      const keyword = this.activeSearchKeyword.toLowerCase();
      if (!keyword) return this.otaList;
      return this.otaList.filter(ota => 
        (ota.firmwareName && ota.firmwareName.toLowerCase().includes(keyword)) ||
        (ota.version && ota.version.toLowerCase().includes(keyword))
      );
    },
    paginatedOtaList() {
      const start = (this.currentPage - 1) * this.pageSize;
      const end = start + this.pageSize;
      return this.filteredOtaList.slice(start, end);
    },
    pageCount() {
      return Math.ceil(this.filteredOtaList.length / this.pageSize);
    },
    visiblePages() {
      const pages = [];
      const maxVisible = 5;
      let start = Math.max(1, this.currentPage - Math.floor(maxVisible / 2));
      let end = Math.min(this.pageCount, start + maxVisible - 1);
      
      if (end - start + 1 < maxVisible) {
        start = Math.max(1, end - maxVisible + 1);
      }
      
      for (let i = start; i <= end; i++) {
        pages.push(i);
      }
      return pages;
    }
  },
  mounted() {
    this.fetchOtaList();
  },
  methods: {
    fetchOtaList() {
      this.loading = true;
      Api.ota.getOtaList({
        page: 1,
        limit: 1000, // 获取所有记录进行前端分页
        firmwareName: this.activeSearchKeyword,
        version: this.activeSearchKeyword
      }, ({ data }) => {
        this.loading = false;
        if (data.code === 0) {
          this.otaList = data.data.list || [];
          this.total = data.data.total || 0;
        } else {
          this.$message.error(data.msg || '获取固件列表失败');
        }
      });
    },
    
    handleSearch() {
      this.activeSearchKeyword = this.searchKeyword;
      this.currentPage = 1;
    },
    
    handleSelectionChange(val) {
      this.selectedOtas = val;
      this.isAllSelected = val.length === this.paginatedOtaList.length;
    },
    
    toggleAllSelection() {
      this.$refs.otaTable.toggleAllSelection();
    },
    
    handleAddOta() {
      this.dialogTitle = '新增固件';
      this.otaForm = {
        id: '',
        firmwareName: '',
        version: '',
        type: '',
        size: null,
        firmwarePath: '',
        remark: '',
        sort: 0
      };
      this.dialogVisible = true;
      this.$nextTick(() => {
        this.$refs.otaForm && this.$refs.otaForm.clearValidate();
      });
    },
    
    handleEdit(row) {
      this.dialogTitle = '编辑固件';
      this.otaForm = JSON.parse(JSON.stringify(row));
      this.dialogVisible = true;
      this.$nextTick(() => {
        this.$refs.otaForm && this.$refs.otaForm.clearValidate();
      });
    },
    
    saveOta() {
      this.$refs.otaForm.validate((valid) => {
        if (!valid) {
          return false;
        }
        
        this.saveLoading = true;
        
        if (this.otaForm.id) {
          // 编辑
          Api.ota.updateOta(this.otaForm, ({ data }) => {
            this.saveLoading = false;
            if (data.code === 0) {
              this.$message.success('修改成功');
              this.dialogVisible = false;
              this.fetchOtaList();
            } else {
              this.$message.error(data.msg || '修改失败');
            }
          });
        } else {
          // 新增
          Api.ota.addOta(this.otaForm, ({ data }) => {
            this.saveLoading = false;
            if (data.code === 0) {
              this.$message.success('新增成功');
              this.dialogVisible = false;
              this.fetchOtaList();
            } else {
              this.$message.error(data.msg || '新增失败');
            }
          });
        }
      });
    },
    
    handleDelete(row) {
      this.$confirm('确认删除该固件记录?', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        Api.ota.deleteOta([row.id], ({ data }) => {
          if (data.code === 0) {
            this.$message.success('删除成功');
            this.fetchOtaList();
          } else {
            this.$message.error(data.msg || '删除失败');
          }
        });
      }).catch(() => {});
    },
    
    deleteSelected() {
      if (this.selectedOtas.length === 0) {
        this.$message.warning('请至少选择一条记录');
        return;
      }
      
      this.$confirm(`确认删除选中的${this.selectedOtas.length}条记录?`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const ids = this.selectedOtas.map(item => item.id);
        Api.ota.deleteOta(ids, ({ data }) => {
          if (data.code === 0) {
            this.$message.success('批量删除成功');
            this.fetchOtaList();
          } else {
            this.$message.error(data.msg || '批量删除失败');
          }
        });
      }).catch(() => {});
    },
    
    handlePageSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
    },
    
    goFirst() {
      this.currentPage = 1;
    },
    
    goPrev() {
      if (this.currentPage > 1) {
        this.currentPage--;
      }
    },
    
    goNext() {
      if (this.currentPage < this.pageCount) {
        this.currentPage++;
      }
    },
    
    goToPage(page) {
      this.currentPage = page;
    },
    
    headerCellClassName({columnIndex}) {
      if (columnIndex === 0) {
        return "custom-selection-header";
      }
      return "";
    },
    
    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
    },
    
    formatFileSize(size) {
      if (!size) return '未知';
      
      const units = ['B', 'KB', 'MB', 'GB'];
      let formattedSize = size;
      let unitIndex = 0;
      
      while (formattedSize >= 1024 && unitIndex < units.length - 1) {
        formattedSize /= 1024;
        unitIndex++;
      }
      
      return `${formattedSize.toFixed(2)} ${units[unitIndex]}`;
    }
  }
};
</script>

<style scoped>
.welcome {
  min-width: 900px;
  min-height: 506px;
  height: 100vh;
  display: flex;
  position: relative;
  flex-direction: column;
  background: linear-gradient(to bottom right, #dce8ff, #e4eeff, #e6cbfd);
  background-size: cover;
  -webkit-background-size: cover;
  -o-background-size: cover;
}

.operation-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
}

.page-title {
  font-size: 24px;
  margin: 0;
  color: #2c3e50;
}

.right-operations {
  display: flex;
  gap: 10px;
  margin-left: auto;
}

.search-input {
  width: 280px;
  border-radius: 4px;
}

.btn-search {
  background: linear-gradient(135deg, #6b8cff, #a966ff);
  border: none;
  color: white;
}

.main-wrapper {
  margin: 5px 22px;
  border-radius: 15px;
  min-height: calc(100vh - 24vh);
  height: auto;
  max-height: 80vh;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  position: relative;
  background: rgba(237, 242, 255, 0.5);
  display: flex;
  flex-direction: column;
}

.content-panel {
  flex: 1;
  display: flex;
  overflow: hidden;
  height: 100%;
  border-radius: 15px;
  background: transparent;
  border: 1px solid #fff;
}

.content-area {
  flex: 1;
  height: 100%;
  min-width: 600px;
  overflow: auto;
  background-color: white;
  display: flex;
  flex-direction: column;
  padding: 20px;
}

.table_bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  flex-wrap: wrap;
}

.ctrl_btn {
  display: flex;
  gap: 8px;
}

.custom-pagination {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-btn {
  min-width: 32px;
  height: 32px;
  background: #f0f2f5;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #606266;
  transition: all 0.3s;
}

.pagination-btn:hover:not(:disabled) {
  color: #409eff;
  border-color: #c6e2ff;
}

.pagination-btn.active {
  color: #409eff;
  border-color: #409eff;
  background: #ecf5ff;
}

.pagination-btn:disabled {
  cursor: not-allowed;
  color: #c0c4cc;
}

.total-text {
  margin-left: 10px;
  color: #606266;
}

.transparent-table {
  width: 100%;
  background: transparent;
}

::v-deep .el-table {
  background-color: transparent;
}

::v-deep .el-table tr {
  background-color: transparent;
}

::v-deep .el-table--enable-row-hover .el-table__body tr:hover > td {
  background-color: #f5f7fa;
}

::v-deep .el-table th {
  background-color: #f5f7fa;
}

::v-deep .el-table__header-wrapper, ::v-deep .el-table__fixed-header-wrapper {
  background-color: #f5f7fa;
}

::v-deep .custom-selection-header .cell {
  padding-right: 10px;
}

.ota-card {
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

::v-deep .el-card__body {
  padding: 20px;
}
</style> 