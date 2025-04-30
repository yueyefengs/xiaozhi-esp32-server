import { getServiceUrl } from '../api';
import RequestService from '../httpRequest';

export default {
    // 获取OTA固件列表
    getOtaList(params, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/ota/page`)
            .method('GET')
            .data(params)
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                console.error('获取固件列表失败:', err);
                RequestService.reAjaxFun(() => {
                    this.getOtaList(params, callback);
                });
            }).send();
    },

    // 获取固件详情
    getOtaDetail(id, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/ota/${id}`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                console.error('获取固件详情失败:', err);
                RequestService.reAjaxFun(() => {
                    this.getOtaDetail(id, callback);
                });
            }).send();
    },

    // 添加OTA固件
    addOta(data, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/ota`)
            .method('POST')
            .data(data)
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                console.error('添加固件失败:', err);
                RequestService.reAjaxFun(() => {
                    this.addOta(data, callback);
                });
            }).send();
    },

    // 更新OTA固件
    updateOta(data, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/ota`)
            .method('PUT')
            .data(data)
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                console.error('更新固件失败:', err);
                RequestService.reAjaxFun(() => {
                    this.updateOta(data, callback);
                });
            }).send();
    },

    // 删除OTA固件
    deleteOta(ids, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/ota`)
            .method('DELETE')
            .data(ids)
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                console.error('删除固件失败:', err);
                RequestService.reAjaxFun(() => {
                    this.deleteOta(ids, callback);
                });
            }).send();
    },
    
    // 获取最新版本固件
    getLatestVersion(callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/ota/latest`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                console.error('获取最新版本固件失败:', err);
                RequestService.reAjaxFun(() => {
                    this.getLatestVersion(callback);
                });
            }).send();
    }
} 