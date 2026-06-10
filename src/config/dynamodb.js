import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import { DynamoDBDocumentClient } from "@aws-sdk/lib-dynamodb";
import dotenv from 'dotenv';

dotenv.config();

const isLocal = process.env.DYNAMODB_ENDPOINT != null;

const clientConfig = {
    region: process.env.AWS_REGION_CUSTOM || process.env.AWS_REGION || 'us-east-1',
};

if (isLocal) {
    clientConfig.endpoint = process.env.DYNAMODB_ENDPOINT;
    clientConfig.credentials = {
        accessKeyId: process.env.AWS_ACCESS_KEY_ID || "dummy",
        secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY || "dummy",
    };
}

const ddbClient = new DynamoDBClient(clientConfig);
const ddbDocClient = DynamoDBDocumentClient.from(ddbClient);

export { ddbClient, ddbDocClient };