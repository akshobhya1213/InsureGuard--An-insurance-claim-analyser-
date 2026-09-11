"""Wraps IBM Watson Natural Language Understanding.

Watson gives us NLP structure (keywords, entities, concepts, sentiment) -
it is NOT a fraud detector. The fraud/suspicion score is computed
separately in fraud_analyzer.py, in plain, explainable Python.
"""

import os

from ibm_cloud_sdk_core.authenticators import IAMAuthenticator
from ibm_watson import NaturalLanguageUnderstandingV1
from ibm_watson.natural_language_understanding_v1 import (
    Features,
    KeywordsOptions,
    EntitiesOptions,
    ConceptsOptions,
    SentimentOptions,
)


class WatsonServiceError(Exception):
    """Raised when Watson NLU cannot be reached or is misconfigured."""


class WatsonService:
    def __init__(self):
        self.api_key = os.getenv("WATSON_API_KEY")
        self.url = os.getenv("WATSON_URL")
        self._client = None

    def _get_client(self):
        if not self.api_key or not self.url:
            raise WatsonServiceError(
                "Watson NLU is not configured. Set WATSON_API_KEY and WATSON_URL in .env"
            )
        if self._client is None:
            authenticator = IAMAuthenticator(self.api_key)
            client = NaturalLanguageUnderstandingV1(version="2022-04-07", authenticator=authenticator)
            client.set_service_url(self.url)
            self._client = client
        return self._client

    def analyze(self, text: str) -> dict:
        """Call Watson NLU and return keywords/entities/concepts/sentiment as plain dicts."""
        client = self._get_client()

        try:
            response = client.analyze(
                text=text,
                features=Features(
                    keywords=KeywordsOptions(limit=10, sentiment=False, emotion=False),
                    entities=EntitiesOptions(limit=10, sentiment=False),
                    concepts=ConceptsOptions(limit=5),
                    sentiment=SentimentOptions(),
                ),
                language="en",
            ).get_result()
        except Exception as exc:
            raise WatsonServiceError(f"Watson NLU request failed: {exc}") from exc

        return {
            "keywords": response.get("keywords", []),
            "entities": response.get("entities", []),
            "concepts": response.get("concepts", []),
            "sentiment": response.get("sentiment", {}),
        }
