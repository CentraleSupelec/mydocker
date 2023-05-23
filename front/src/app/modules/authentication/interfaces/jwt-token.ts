export interface IJWTToken {
  encodedToken: string;
  decodedToken: {
    exp: number;
    iat: number;
    iss: string;
    jti: string;
    nbf: number;
    sub: string;
  };
  expirationDate: Date;
}
