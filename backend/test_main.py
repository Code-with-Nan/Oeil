import os
import tempfile
import unittest
from types import SimpleNamespace
from unittest.mock import AsyncMock, patch

from fastapi.testclient import TestClient

from auth import _hash_password, _verify_password
from main import (
    DEFAULT_MODEL,
    AgentStepRequest,
    ChatRequest,
    NVIDIA_TOOLS,
    app,
    nvidia_agent_messages,
    nvidia_chat_messages,
    nvidia_model_name,
)


class BackendTests(unittest.TestCase):
    def test_health_is_available_without_provider_credentials(self):
        with patch.dict(os.environ, {"NVIDIA_API_KEY": ""}):
            response = TestClient(app).get("/health")
            self.assertEqual(response.status_code, 200)
            self.assertEqual(response.json()["status"], "ok")
            self.assertEqual(response.json()["model"], DEFAULT_MODEL)
            self.assertFalse(response.json()["ai_configured"])

    def test_chat_reports_missing_nvidia_key(self):
        with patch.dict(os.environ, {"NVIDIA_API_KEY": ""}):
            response = TestClient(app).post(
                "/chat",
                json={"mode": "AUTO", "messages": [{"role": "user", "content": "Hi"}]},
            )
        self.assertEqual(response.status_code, 503)
        self.assertIn("NVIDIA_API_KEY", response.json()["detail"])

    def test_mode_selects_nvidia_instruction_temperature_and_roles(self):
        request = ChatRequest(
            mode="PRECISE",
            messages=[
                {"role": "user", "content": "Explain this briefly."},
                {"role": "assistant", "content": "A short answer."},
            ],
        )
        messages = nvidia_chat_messages(request)

        self.assertEqual(messages[0]["role"], "system")
        self.assertIn("direct, factual, concise", messages[0]["content"])
        self.assertEqual(messages[1]["role"], "user")
        self.assertEqual(messages[2]["role"], "assistant")

    def test_agent_payload_offers_only_bounded_android_actions(self):
        request = AgentStepRequest(
            task="Open settings",
            screen=[{"id": "e0", "label": "Settings", "clickable": True, "editable": False}],
            apps=[{"label": "Settings", "package_name": "com.android.settings"}],
            messages=[{"role": "user", "content": "Open settings"}],
        )

        messages = nvidia_agent_messages(request)
        declarations = [tool["function"] for tool in NVIDIA_TOOLS]

        self.assertEqual(
            {function["name"] for function in declarations},
            {
                "tap",
                "type",
                "scroll",
                "back",
                "home",
                "recents",
                "notifications",
                "open_app",
                "open_url",
            },
        )
        self.assertIn("untrusted data", messages[0]["content"])
        self.assertIn("com.android.settings", messages[-1]["content"])
        self.assertIn("Open settings", messages[-1]["content"])

    def test_blank_model_environment_falls_back_to_the_working_default(self):
        with patch.dict(os.environ, {"NVIDIA_MODEL": ""}):
            self.assertEqual(nvidia_model_name(), DEFAULT_MODEL)

    def test_open_url_action_is_validated(self):
        fake_choice = SimpleNamespace(
            message=SimpleNamespace(
                content=None,
                tool_calls=[
                    SimpleNamespace(
                        function=SimpleNamespace(
                            name="open_url",
                            arguments='{"url": "https://example.com", "summary": "Open example.com"}',
                        )
                    )
                ],
            )
        )
        mock_client = AsyncMock()
        mock_client.chat.completions.create.return_value = SimpleNamespace(
            choices=[fake_choice]
        )

        with patch.dict(os.environ, {"NVIDIA_API_KEY": "test-key"}), patch(
            "main.get_nvidia_client", return_value=mock_client
        ):
            response = TestClient(app).post(
                "/agent/step",
                json={"task": "Open example.com", "screen": []},
            )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["action"]["url"], "https://example.com")

    def test_agent_step_returns_validated_nvidia_function_call(self):
        fake_choice = SimpleNamespace(
            message=SimpleNamespace(
                content=None,
                tool_calls=[
                    SimpleNamespace(
                        function=SimpleNamespace(
                            name="tap",
                            arguments='{"target_id": "e0", "target_label": "Settings", "summary": "Open Settings"}',
                        )
                    )
                ],
            )
        )
        mock_client = AsyncMock()
        mock_client.chat.completions.create.return_value = SimpleNamespace(
            choices=[fake_choice]
        )

        with patch.dict(os.environ, {"NVIDIA_API_KEY": "test-key"}), patch(
            "main.get_nvidia_client", return_value=mock_client
        ):
            response = TestClient(app).post(
                "/agent/step",
                json={
                    "task": "Open settings",
                    "screen": [{
                        "id": "e0",
                        "label": "Settings",
                        "clickable": True,
                        "editable": False,
                    }],
                },
            )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {
            "type": "action",
            "action": {
                "type": "tap",
                "target_id": "e0",
                "target_label": "Settings",
                "summary": "Open Settings",
            },
        })

    def test_agent_step_rejects_malformed_function_call(self):
        fake_choice = SimpleNamespace(
            message=SimpleNamespace(
                content=None,
                tool_calls=[
                    SimpleNamespace(
                        function=SimpleNamespace(
                            name="tap",
                            arguments='{"summary": "Tap"}',
                        )
                    )
                ],
            )
        )
        mock_client = AsyncMock()
        mock_client.chat.completions.create.return_value = SimpleNamespace(
            choices=[fake_choice]
        )

        with patch.dict(os.environ, {"NVIDIA_API_KEY": "test-key"}), patch(
            "main.get_nvidia_client", return_value=mock_client
        ):
            response = TestClient(app).post(
                "/agent/step",
                json={"task": "Tap", "screen": []},
            )

        self.assertEqual(response.status_code, 502)
        self.assertIn("invalid action", response.json()["detail"])

    def test_signup_login_me_and_logout(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(
            os.environ,
            {
                "OEIL_AUTH_DB_PATH": f"{directory}/auth.sqlite3",
                "OEIL_AUTH_SECRET": "test-auth-secret-that-is-at-least-32-characters",
            },
        ):
            client = TestClient(app)
            signup = client.post(
                "/auth/signup",
                json={"name": "Ada Lovelace", "email": " ADA@example.com ", "password": "safe-password"},
            )
            self.assertEqual(signup.status_code, 200)
            account = signup.json()
            self.assertEqual(account["user"]["email"], "ada@example.com")
            self.assertNotIn("password", account)

            login = client.post(
                "/auth/login",
                json={"email": "ada@example.com", "password": "safe-password"},
            )
            self.assertEqual(login.status_code, 200)
            headers = {"Authorization": f"Bearer {login.json()['token']}"}
            self.assertEqual(client.get("/auth/me", headers=headers).json()["name"], "Ada Lovelace")
            self.assertEqual(client.post("/auth/logout", headers=headers).status_code, 200)
            self.assertEqual(client.get("/auth/me", headers=headers).status_code, 401)

    def test_signup_rejects_duplicate_email_and_bad_login(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(
            os.environ,
            {
                "OEIL_AUTH_DB_PATH": f"{directory}/auth.sqlite3",
                "OEIL_AUTH_SECRET": "test-auth-secret-that-is-at-least-32-characters",
            },
        ):
            client = TestClient(app)
            body = {"name": "Ada", "email": "ada@example.com", "password": "safe-password"}
            self.assertEqual(client.post("/auth/signup", json=body).status_code, 200)
            self.assertEqual(client.post("/auth/signup", json=body).status_code, 409)
            bad_login = client.post(
                "/auth/login",
                json={"email": "ada@example.com", "password": "wrong-password"},
            )
            self.assertEqual(bad_login.status_code, 401)

    def test_auth_is_disabled_without_a_session_secret(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(
            os.environ,
            {"OEIL_AUTH_DB_PATH": f"{directory}/auth.sqlite3"},
            clear=True,
        ):
            response = TestClient(app).post(
                "/auth/signup",
                json={"name": "Ada", "email": "ada@example.com", "password": "safe-password"},
            )
        self.assertEqual(response.status_code, 503)

    def test_password_hashes_are_salted_and_verified(self):
        first = _hash_password("safe-password")
        second = _hash_password("safe-password")
        self.assertNotEqual(first, second)
        self.assertTrue(_verify_password("safe-password", first))
        self.assertFalse(_verify_password("wrong-password", first))

    def test_oauth_start_requires_provider_configuration(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(
            os.environ,
            {"OEIL_AUTH_DB_PATH": f"{directory}/auth.sqlite3"},
            clear=True,
        ):
            response = TestClient(app).get("/auth/oauth/google/start")
        self.assertEqual(response.status_code, 503)

    def test_oauth_start_uses_a_fixed_app_redirect(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(
            os.environ,
            {
                "OEIL_AUTH_DB_PATH": f"{directory}/auth.sqlite3",
                "OEIL_PUBLIC_BASE_URL": "https://api.example.test",
                "GOOGLE_CLIENT_ID": "test-client",
                "GOOGLE_CLIENT_SECRET": "test-secret",
            },
        ):
            client = TestClient(app)
            invalid = client.get(
                "/auth/oauth/google/start",
                params={"redirect_uri": "https://attacker.example"},
            )
            response = client.get("/auth/oauth/google/start", follow_redirects=False)
        self.assertEqual(invalid.status_code, 400)
        self.assertEqual(response.status_code, 302)
        self.assertIn("accounts.google.com", response.headers["location"])
        self.assertIn("https%3A%2F%2Fapi.example.test%2Fauth%2Foauth%2Fgoogle%2Fcallback", response.headers["location"])


if __name__ == "__main__":
    unittest.main()
