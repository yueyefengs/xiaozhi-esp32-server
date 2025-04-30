import asyncio
import sys
import signal
from config.settings import load_config, check_config_file
from core.websocket_server import WebSocketServer
from core.utils.util import check_ffmpeg_installed

TAG = __name__


async def wait_for_exit():
    """Windows 和 Linux 兼容的退出监听"""
    loop = asyncio.get_running_loop()
    stop_event = asyncio.Event()

    if sys.platform == "win32":
        # Windows: 用 sys.stdin.read() 监听 Ctrl + C
        await loop.run_in_executor(None, sys.stdin.read)
    else:
        # Linux/macOS: 用 signal 监听 Ctrl + C
        def stop():
            stop_event.set()

        loop.add_signal_handler(signal.SIGINT, stop)
        loop.add_signal_handler(signal.SIGTERM, stop)  # 支持 kill 进程
        await stop_event.wait()


async def main():
    check_config_file()
    check_ffmpeg_installed()
    config = load_config()
    print('Starting WebSocket server for debugging...')
    ws_server = WebSocketServer(config)
    try:
        await ws_server.start()
    except Exception as e:
        print(f'Error during server start: {e}')
    finally:
        print('Server has stopped or encountered an issue.')
        await asyncio.sleep(0.1)


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("手动中断，程序终止。")
