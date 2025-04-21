import os
import uuid
import json
import base64
import requests
from datetime import datetime
from core.utils.util import check_model_key
from core.providers.tts.base import TTSProviderBase
from config.logger import setup_logging

TAG = __name__
logger = setup_logging()


class TTSProvider(TTSProviderBase):
    def __init__(self, config, delete_audio_file):
        super().__init__(config, delete_audio_file)
        # 基础配置
        self.appid = config.get("appid", "6678992238")  # 设置默认值
        self.access_token = config.get("access_token", "").strip()
        self.cluster = config.get("cluster", "volcano_tts")  # 默认值
        self.voice = config.get("private_voice") if config.get("private_voice") else config.get("voice", "")
        self.api_url = config.get("api_url", "")
        self.authorization = config.get("authorization", "").strip()
        
        # 记录基础配置
        logger.bind(tag=TAG).info(
            f"TTS基础配置:\n"
            f"API URL: {self.api_url}\n"
            f"Voice: {self.voice}\n"
            f"AppID: {self.appid}\n"
            f"Authorization: '{self.authorization}'\n"
            f"Token Length: {len(self.access_token)}"
        )
        
        # 验证必要配置
        if not self.api_url:
            logger.bind(tag=TAG).error("TTS配置错误: 未设置 API URL")
        if not self.access_token:
            logger.bind(tag=TAG).error("TTS配置错误: 未设置 access_token")
        
        # 初始化请求头 - 使用分号分隔 Bearer 和 token
        if self.authorization:
            # 确保使用分号分隔
            auth_base = self.authorization.rstrip(';')  # 移除末尾可能存在的分号
            auth_value = f"{auth_base};{self.access_token}"
            
            self.header = {
                "Content-Type": "application/json; charset=utf-8",
                "Authorization": auth_value
            }
        else:
            # 没有指定 authorization 格式，使用标准格式
            self.header = {
                "Content-Type": "application/json; charset=utf-8",
                "Authorization": f"Bearer;{self.access_token}"
            }
        
        logger.bind(tag=TAG).info(f"请求头: {json.dumps(self.header, ensure_ascii=False)}")
        check_model_key("TTS", self.access_token)

    def generate_filename(self, extension=".wav"):
        return os.path.join(
            self.output_file,
            f"tts-{datetime.now().date()}@{uuid.uuid4().hex}{extension}",
        )

    async def text_to_speak(self, text, output_file):
        # 构建请求体
        request_json = {
            "app": {
                "appid": self.appid,
                "token": self.access_token,
                "cluster": self.cluster,
            },
            "user": {"uid": "1"},
            "audio": {
                "voice_type": self.voice,
                "encoding": "wav",
                "speed_ratio": 1.0,
                "volume_ratio": 1.0,
                "pitch_ratio": 1.0,
            },
            "request": {
                "reqid": str(uuid.uuid4()),
                "text": text,
                "text_type": "plain",
                "operation": "query",
                "with_frontend": 1,
                "frontend_type": "unitTson",
            },
        }
        
        # 记录请求信息
        logger.bind(tag=TAG).info(
            f"TTS请求信息:\n"
            f"Text: {text}\n"
            f"API URL: {self.api_url}\n"
            f"Authorization: '{self.header['Authorization']}'\n"
            f"请求体: {json.dumps(request_json, ensure_ascii=False)}"
        )

        try:
            # 发送请求
            resp = requests.post(
                self.api_url, 
                json=request_json,  # 直接使用 json 参数，让 requests 自动处理序列化
                headers=self.header, 
                timeout=10
            )
            
            # 记录响应信息
            logger.bind(tag=TAG).info(
                f"TTS响应信息:\n"
                f"状态码: {resp.status_code}\n"
                f"响应头: {dict(resp.headers)}\n"
                f"响应内容: {resp.text[:200] + '...' if len(resp.text) > 200 else resp.text}"
            )

            # 判断响应是否成功
            if resp.status_code == 200:
                try:
                    resp_json = resp.json()
                    
                    # 火山引擎格式 (Base64 编码的音频)
                    if "data" in resp_json:
                        data = resp_json["data"]
                        with open(output_file, "wb") as audio_file:
                            audio_file.write(base64.b64decode(data))
                        logger.bind(tag=TAG).info(f"TTS音频文件已保存: {output_file}")
                        return
                except Exception as e:
                    error_msg = f"解析响应失败: {str(e)}"
                    logger.bind(tag=TAG).error(error_msg)
            
            # 处理错误
            error_msg = f"TTS请求失败: 状态码: {resp.status_code}, 响应内容: {resp.text}"
            logger.bind(tag=TAG).error(error_msg)
            raise Exception(error_msg)
                
        except Exception as e:
            error_msg = f"TTS转换异常: {str(e)}"
            logger.bind(tag=TAG).error(error_msg)
            raise Exception(f"{__name__} error: {error_msg}")
