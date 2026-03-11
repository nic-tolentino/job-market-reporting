# Getting a Gemini API Key

## Quick Guide

The crawler service requires a Google Gemini API key to extract job data from career pages.

### Option 1: Google AI Studio (Recommended for Development)

1. **Go to**: https://aistudio.google.com/app/apikey
2. **Sign in** with your Google account
3. **Click "Create API Key"**
4. **Select a project** or create a new one
5. **Copy the API key** (starts with `AIza...`)

**Free Tier:**
- 60 requests per minute
- 1,500 requests per day
- Enough for testing and development

### Option 2: Google Cloud Vertex AI (Recommended for Production)

1. **Go to**: https://console.cloud.google.com/vertex-ai
2. **Enable the Vertex AI API**
3. **Create an API Key** in Cloud Console
4. **Set up billing** (pay-as-you-go)

**Production Pricing (Gemini 2.0 Flash):**
- Input: ~$0.075 per 1M tokens
- Output: ~$0.30 per 1M tokens
- Average crawl: ~$0.0001-0.0005 per company

### Add to Your .env File

Once you have the key, add it to `.env`:

```bash
# Crawler Service Configuration
GEMINI_API_KEY=AIzaSy...your-key-here
```

### Add to Secret Manager (For Production)

```bash
# Create secret
gcloud secrets create GEMINI_API_KEY \
    --project=tech-market-insights \
    --replication-policy="automatic"

# Add version
echo "AIzaSy...your-key-here" | gcloud secrets versions add GEMINI_API_KEY \
    --project=tech-market-insights \
    --data-file=-
```

### Test Your API Key

```bash
# Test with curl
curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=YOUR_API_KEY" \
    -H 'Content-Type: application/json' \
    -d '{
      "contents": [{
        "parts": [{
          "text": "Hello"
        }]
      }]
    }'
```

### Expected Response

```json
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "Hello! How can I help you today?"
          }
        ]
      }
    }
  ]
}
```

## Cost Estimates

### Development Testing
- **100 crawls/day**: Free tier sufficient
- **500 crawls/day**: ~$0.05-0.25/day

### Production (1,257 companies)
- **Full crawl (all companies)**: ~$0.13-0.63
- **Weekly refresh**: ~$0.50-2.50/week
- **Monthly cost**: ~$2-10/month

## Next Steps

1. ✅ Get your API key from Google AI Studio
2. ✅ Add to `.env` file
3. ✅ Add to Secret Manager
4. ✅ Deploy crawler service
5. ✅ Test with a few companies

## Troubleshooting

### "API_KEY_INVALID" Error
- Double-check you copied the full key
- Ensure no extra spaces
- Try regenerating the key

### "BILLING_NOT_ACTIVE" Error
- Enable billing in Google Cloud Console
- Add a payment method

### "QUOTA_EXCEEDED" Error
- Upgrade to paid tier
- Request quota increase

## Security Notes

⚠️ **Never commit API keys to git**
- Already in `.gitignore` ✅
- Use Secret Manager for production ✅
- Rotate keys periodically

## Links

- [Google AI Studio](https://aistudio.google.com/)
- [Gemini API Documentation](https://ai.google.dev/api)
- [Pricing Calculator](https://cloud.google.com/products/calculator)
- [Vertex AI Pricing](https://cloud.google.com/vertex-ai/pricing)
