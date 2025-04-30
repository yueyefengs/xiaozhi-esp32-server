import os
import time
from typing import Optional, Dict

import httpx
import logging

TAG = __name__


class DeviceNotFoundException(Exception):
    pass


class DeviceBindException(Exception):
    def __init__(self, bind_code):
        self.bind_code = bind_code
        super().__init__(f"设备绑定异常，绑定码: {bind_code}")


class ManageApiClient:
    _instance = None
    _client = None
    _secret = None

    def __new__(cls, config):
        """单例模式确保全局唯一实例，并支持传入配置参数"""
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._init_client(config)
        return cls._instance

    @classmethod
    def _init_client(cls, config):
        """初始化持久化连接池"""
        cls.config = config.get("manager-api")

        if not cls.config:
            raise Exception("manager-api配置错误")

        if not cls.config.get("url") or not cls.config.get("secret"):
            raise Exception("manager-api的url或secret配置错误")

        if "你" in cls.config.get("secret"):
            raise Exception("请先配置manager-api的secret")

        cls._secret = cls.config.get("secret")
        cls.max_retries = cls.config.get("max_retries", 6)  # 最大重试次数
        cls.retry_delay = cls.config.get("retry_delay", 10)  # 初始重试延迟(秒)
        # NOTE(goody): 2025/4/16 http相关资源统一管理，后续可以增加线程池或者超时
        # 后续也可以统一配置apiToken之类的走通用的Auth
        cls._client = httpx.Client(
            base_url=cls.config.get("url"),
            headers={
                "User-Agent": f"PythonClient/2.0 (PID:{os.getpid()})",
                "Accept": "application/json",
            },
            timeout=cls.config.get("timeout", 30),  # 默认超时时间30秒
        )

    @classmethod
    def _request(cls, method: str, endpoint: str, **kwargs) -> Dict:
        """发送单次HTTP请求并处理响应"""
        endpoint = endpoint.lstrip("/")
        logger = logging.getLogger(__name__)
        curl_cmd = f"curl -X {method} {cls._client.base_url}{endpoint}"
        if 'headers' not in kwargs:
            kwargs['headers'] = {}
        kwargs['headers']['Content-Type'] = 'application/json; charset=UTF-8'  # Force JSON Content-Type
        if 'headers' in kwargs:
            for key, value in kwargs.get('headers', {}).items():
                curl_cmd += f" -H '{key}: {value}'"
        if 'json' in kwargs:
            curl_cmd += f" -d '{str(kwargs.get('json'))}'"  # Simple string representation
        logger.error(f"API Request as curl: {curl_cmd}")  # Log the curl command
        response = cls._client.request(method, endpoint, **kwargs)
        response.raise_for_status()
        logger.error(f'API returned parameters: {response.json()}')
        result = response.json()

        # 处理API返回的业务错误
        if result.get("code") == 10041:
            raise DeviceNotFoundException(result.get("msg"))
        elif result.get("code") == 10042:
            raise DeviceBindException(result.get("msg"))
        elif result.get("code") != 0:
            raise Exception(f"API返回错误: {result.get('msg', '未知错误')}")

        # 返回成功数据
        return result.get("data") if result.get("code") == 0 else None

    @classmethod
    def _should_retry(cls, exception: Exception) -> bool:
        """判断异常是否应该重试"""
        # 网络连接相关错误
        if isinstance(
            exception, (httpx.ConnectError, httpx.TimeoutException, httpx.NetworkError)
        ):
            return True

        # HTTP状态码错误
        if isinstance(exception, httpx.HTTPStatusError):
            status_code = exception.response.status_code
            return status_code in [408, 429, 500, 502, 503, 504]

        return False

    @classmethod
    def _execute_request(cls, method: str, endpoint: str, **kwargs) -> Dict:
        """带重试机制的请求执行器"""
        retry_count = 0

        while retry_count <= cls.max_retries:
            try:
                # 执行请求
                return cls._request(method, endpoint, **kwargs)
            except Exception as e:
                # 判断是否应该重试
                if retry_count < cls.max_retries and cls._should_retry(e):
                    retry_count += 1
                    print(
                        f"{method} {endpoint} 请求失败，将在 {cls.retry_delay:.1f} 秒后进行第 {retry_count} 次重试"
                    )
                    time.sleep(cls.retry_delay)
                    continue
                else:
                    # 不重试，直接抛出异常
                    raise

    @classmethod
    def safe_close(cls):
        """安全关闭连接池"""
        if cls._client:
            cls._client.close()
            cls._instance = None


def get_server_config() -> Optional[Dict]:
    """获取服务器基础配置"""
    return ManageApiClient._instance._execute_request(
        "POST", "/config/server-base", json={"secret": ManageApiClient._secret}
    )


def get_agent_models(
    mac_address: str, client_id: str, selected_module: Dict
) -> Optional[Dict]:
    """获取代理模型配置"""
    return ManageApiClient._instance._execute_request(
        "POST",
        "/config/agent-models",
        json={
            "secret": ManageApiClient._secret,
            "macAddress": mac_address,
            "clientId": client_id,
            "selectedModule": selected_module,
        },
    )


def init_service(config):
    ManageApiClient(config)


def manage_api_http_safe_close():
    ManageApiClient.safe_close()
