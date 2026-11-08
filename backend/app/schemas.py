"""Pydantic request and response schemas for the Oeil API."""

from typing import Literal

from pydantic import BaseModel, Field

Mode = Literal["AUTO", "PRECISE", "CREATIVE"]
Role = Literal["user", "assistant"]
ActionType = Literal[
    "tap",
    "type",
    "scroll",
    "back",
    "home",
    "recents",
    "notifications",
    "open_app",
    "open_url",
]


class ChatMessage(BaseModel):
    role: Role
    content: str = Field(min_length=1, max_length=16_000)


class ChatRequest(BaseModel):
    messages: list[ChatMessage] = Field(min_length=1, max_length=50)
    mode: Mode = "AUTO"


class ScreenElement(BaseModel):
    id: str = Field(min_length=1, max_length=12)
    label: str = Field(max_length=500)
    clickable: bool
    editable: bool


class InstalledApp(BaseModel):
    label: str = Field(min_length=1, max_length=120)
    package_name: str = Field(min_length=1, max_length=200)


class AgentStepRequest(BaseModel):
    task: str = Field(min_length=1, max_length=4_000)
    screen: list[ScreenElement] = Field(default_factory=list, max_length=100)
    apps: list[InstalledApp] = Field(default_factory=list, max_length=300)
    messages: list[ChatMessage] = Field(default_factory=list, max_length=30)
    mode: Mode = "AUTO"


class AgentAction(BaseModel):
    type: ActionType
    summary: str = Field(min_length=1, max_length=300)
    target_id: str | None = Field(default=None, max_length=12)
    target_label: str | None = Field(default=None, max_length=500)
    text: str | None = Field(default=None, max_length=2_000)
    direction: Literal["forward", "backward"] | None = None
    package_name: str | None = Field(default=None, max_length=200)
    app_name: str | None = Field(default=None, max_length=120)
    url: str | None = Field(default=None, max_length=2_000)
